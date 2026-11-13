package eu.kanade.tachiyomi.extension.en.nhapi

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NHentaiApi : HttpSource() {

    override val name = "NHentai (API v2)"

    override val baseUrl = "https://nhentai.net"

    private val apiUrl = "$baseUrl/api/v2"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(3, 1, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Authorization", "Bearer $API_KEY")
        .add("Accept", "application/json")

    // ─────────────────────── POPULAR ───────────────────────

    override fun popularMangaRequest(page: Int): Request =
        searchMangaRequest(page, "", FilterList(SortFilter(1)))

    override fun popularMangaParse(response: Response): MangasPage =
        searchMangaParse(response)

    // ─────────────────────── LATEST ───────────────────────

    override fun latestUpdatesRequest(page: Int): Request =
        searchMangaRequest(page, "", FilterList(SortFilter(0)))

    override fun latestUpdatesParse(response: Response): MangasPage =
        searchMangaParse(response)

    // ─────────────────────── SEARCH ───────────────────────

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // Deep-link support: "id:123456" opens the gallery directly
        if (query.startsWith("id:")) {
            return GET("$apiUrl/galleries/" + query.removePrefix("id:"), headers)
        }

        val url = "$apiUrl/galleries/search".toHttpUrlBuilderCompat().apply {
            addQueryParameter("query", query)
            addQueryParameter("page", page.toString())
        }

        filters.forEach { filter ->
            when (filter) {
                is SortFilter -> url.addQueryParameter("sort", filter.toUriPart())
                else -> {}
            }
        }
        return GET(url.build(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        // Single-gallery response (from id: query) — shape has no "result" array
        val body = response.body?.string() ?: ""
        val json = JSONObject(body)
        if (!json.has("result")) {
            val manga = galleryToSManga(json)
            return MangasPage(listOf(manga), hasNextPage = false)
        }

        val page = response.request.url.queryParameter("page")?.toIntOrNull() ?: 1
        val numPages = json.optInt("num_pages", page)
        val arr: JSONArray = json.getJSONArray("result")
        val entries = (0 until arr.length()).map { galleryToSManga(arr.getJSONObject(it)) }
        return MangasPage(entries, hasNextPage = page < numPages)
    }

    private fun galleryToSManga(g: JSONObject) = SManga.create().apply {
        val titles = g.getJSONObject("title")
        title = titles.optString("pretty").ifBlank { titles.optString("english") }
        url = "/api/v2/galleries/" + g.getInt("id")

        thumbnail_url = imageUrl(g.getString("media_id"), g.getJSONObject("images"), "thumbnail", 1)

        genre = g.getJSONArray("tags").let { arr ->
            (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .filter { it.getString("type") == "category" }
                .joinToString(", ") { it.getString("name") }
        }
        status = SManga.COMPLETED
        description = buildString {
            append("ID: ").append(g.getInt("id"))
            append("\nPages: ").append(g.getInt("num_pages"))
            append("\nUploaded: ").append(formatDate(g.optLong("upload_date", 0L)))
            val scanlator = g.optString("scanlator")
            if (scanlator.isNotBlank()) append("\nScanlator: ").append(scanlator)
        }
    }

    // ─────────────────────── DETAILS ───────────────────────

    override fun mangaDetailsRequest(manga: SManga): Request =
        GET(baseUrl + manga.url, headers)

    override fun mangaDetailsParse(response: Response): SManga =
        galleryToSManga(JSONObject(response.body?.string() ?: ""))

    // ─────────────────────── CHAPTERS ───────────────────────

    override fun chapterListRequest(manga: SManga): Request =
        mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val gallery = JSONObject(response.body?.string() ?: "")
        return listOf(
            SChapter.create().apply {
                name = "Read"
                url = "/api/v2/galleries/" + gallery.getInt("id")
                date_upload = gallery.optLong("upload_date", 0L) * 1000
            },
        )
    }

    // ─────────────────────── PAGES ───────────────────────

    override fun pageListRequest(chapter: SChapter): Request =
        GET(baseUrl + chapter.url, headers)

    override fun pageListParse(response: Response): List<Page> {
        val gallery = JSONObject(response.body?.string() ?: "")
        val mediaId = gallery.getString("media_id")
        val pages: JSONArray = gallery.getJSONObject("images").getJSONArray("pages")
        return (0 until pages.length()).map { i ->
            val ext = extFromType(pages.getJSONObject(i).getString("t"))
            Page(i, imageUrl = "https://i.nhentai.net/galleries/$mediaId/${i + 1}.$ext")
        }
    }

    override fun imageUrlParse(response: Response): String =
        throw UnsupportedOperationException("Not used")

    // ─────────────────────── HELPERS ───────────────────────

    private fun imageUrl(mediaId: String, images: JSONObject, kind: String, page: Int): String {
        val info = images.getJSONObject(kind)
        return "https://t.nhentai.net/galleries/$mediaId/${kind}.$page.${extFromType(info.getString("t"))}"
    }

    private fun extFromType(t: String) = when (t) {
        "p" -> "png"
        "g" -> "gif"
        "w" -> "webp"
        else -> "jpg" // "j"
    }

    private fun formatDate(epochSec: Long): String =
        if (epochSec > 0) DATE_FORMAT.format(Date(epochSec * 1000)) else "unknown"

    private fun String.toHttpUrlBuilderCompat(): HttpUrl.Builder =
        this.toHttpUrl().newBuilder()

    companion object {
        // ═══════════════════════════════════════════════════════════════════
        //   👇 1. PASTE YOUR API KEY HERE 👇
        private const val API_KEY = "nhk_GLndZseklvCuu5m8iB7QAKMiOAp3gej6B1h_3BCMSHY518km"
        //   👆 1. PASTE YOUR API KEY HERE 👆
        // ═══════════════════════════════════════════════════════════════════

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    }

    // ─────────────────────── FILTERS ───────────────────────

    override fun getFilterList() = FilterList(SortFilter())

    fun getFilterList(sortIndex: Int) = FilterList(SortFilter(sortIndex))

    internal class SortFilter(selection: Int = 0) : UriPartFilter(
        "Sort by",
        arrayOf(
            Pair("Newest", "date"),
            Pair("Popular", "popular"),
            Pair("Popular today", "popular-today"),
            Pair("Popular this week", "popular-week"),
        ),
        selection,
    )

    internal open class UriPartFilter(
        displayName: String,
        private val vals: Array<Pair<String, String>>,
        defaultSelection: Int = 0,
    ) : Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray(), defaultSelection) {
        fun toUriPart() = vals[state].second
    }
}
