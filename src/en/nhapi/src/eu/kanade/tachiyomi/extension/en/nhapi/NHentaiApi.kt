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
import okhttp3.Headers
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

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Authorization", "Bearer $API_KEY")
        .add("Accept", "application/json")

    // Clean headers without Auth & Accept JSON for image loading
    override fun imageRequest(page: Page): Request {
        val cleanHeaders = headers.newBuilder()
            .removeAll("Authorization")
            .removeAll("Accept")
            .build()
        return GET(page.imageUrl!!, cleanHeaders)
    }

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
        val cleanQuery = query.trim()
        val id = cleanQuery.removePrefix("id:").removePrefix("/g/").trim()
        if (id.isNotEmpty() && id.all { it.isDigit() }) {
            return GET("$apiUrl/galleries/$id", headers)
        }

        val url = "$apiUrl/galleries/search".toHttpUrlBuilderCompat().apply {
            addQueryParameter("query", if (cleanQuery.isBlank()) "*" else cleanQuery)
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
        val body = response.body?.string() ?: ""
        if (body.isBlank()) return MangasPage(emptyList(), false)

        val json = JSONObject(body)
        // Single-gallery response (from direct ID query) — shape has no "result" array
        if (!json.has("result")) {
            if (json.has("id")) {
                val manga = galleryToSManga(json)
                return MangasPage(listOf(manga), hasNextPage = false)
            }
            return MangasPage(emptyList(), false)
        }

        val page = response.request.url.queryParameter("page")?.toIntOrNull() ?: 1
        val numPages = json.optInt("num_pages", page)
        val arr: JSONArray = json.optJSONArray("result") ?: JSONArray()
        val entries = (0 until arr.length()).map { galleryToSManga(arr.getJSONObject(it)) }
        return MangasPage(entries, hasNextPage = page < numPages)
    }

    private fun galleryToSManga(g: JSONObject) = SManga.create().apply {
        val titles = g.optJSONObject("title")
        val id = g.getInt("id")
        title = titles?.optString("pretty")?.ifBlank { titles.optString("english") } ?: id.toString()
        url = "/g/$id/"

        val mediaId = g.getString("media_id")
        val images = g.getJSONObject("images")
        thumbnail_url = imageUrl(mediaId, images, "thumbnail")

        val tagsArr = g.optJSONArray("tags") ?: JSONArray()
        val tagsList = (0 until tagsArr.length()).map { tagsArr.getJSONObject(it) }

        artist = tagsList.filter { it.optString("type") == "artist" }
            .joinToString(", ") { it.optString("name") }
            .ifBlank {
                tagsList.filter { it.optString("type") == "group" }
                    .joinToString(", ") { it.optString("name") }
            }
        author = artist

        genre = tagsList
            .filter { it.optString("type") in setOf("tag", "category", "parody", "character") }
            .joinToString(", ") { it.optString("name") }

        status = SManga.COMPLETED
        description = buildString {
            append("ID: ").append(id)
            append("\nPages: ").append(g.optInt("num_pages", 0))
            append("\nUploaded: ").append(formatDate(g.optLong("upload_date", 0L)))
            val scanlator = g.optString("scanlator")
            if (scanlator.isNotBlank()) append("\nScanlator: ").append(scanlator)
        }
    }

    // ─────────────────────── DETAILS ───────────────────────

    override fun mangaDetailsRequest(manga: SManga): Request {
        val id = manga.url.split("/").filter { it.isNotBlank() }.last()
        return GET("$apiUrl/galleries/$id", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga =
        galleryToSManga(JSONObject(response.body?.string() ?: ""))

    // ─────────────────────── CHAPTERS ───────────────────────

    override fun chapterListRequest(manga: SManga): Request =
        mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val gallery = JSONObject(response.body?.string() ?: "")
        val id = gallery.getInt("id")
        return listOf(
            SChapter.create().apply {
                name = "Chapter"
                url = "/g/$id/"
                chapter_number = 1f
                date_upload = gallery.optLong("upload_date", 0L) * 1000
            },
        )
    }

    // ─────────────────────── PAGES ───────────────────────

    override fun pageListRequest(chapter: SChapter): Request {
        val id = chapter.url.split("/").filter { it.isNotBlank() }.last()
        return GET("$apiUrl/galleries/$id", headers)
    }

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

    private fun imageUrl(mediaId: String, images: JSONObject, kind: String, page: Int = 1): String {
        val info = images.optJSONObject(kind) ?: return ""
        val ext = extFromType(info.optString("t"))
        return when (kind) {
            "cover" -> "https://t.nhentai.net/galleries/$mediaId/cover.$ext"
            "thumbnail" -> "https://t.nhentai.net/galleries/$mediaId/thumb.$ext"
            else -> "https://t.nhentai.net/galleries/$mediaId/${page}t.$ext"
        }
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
        private const val API_KEY = "nhk_GLndZseklvCuu5m8iB7QAKMiOAp3gej6B1h_3BCMSHY518km"

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
