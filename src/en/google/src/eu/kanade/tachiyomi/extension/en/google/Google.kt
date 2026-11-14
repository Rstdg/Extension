package eu.kanade.tachiyomi.extension.en.google

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.net.URLEncoder

class Google : HttpSource() {

    override val name = "Google"

    override val baseUrl = "https://www.google.com"

    override val lang = "en"

    override val supportsLatest = false

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/search?q=manga", headers)

    override fun popularMangaParse(response: Response): MangasPage =
        searchMangaParse(response)

    override fun latestUpdatesRequest(page: Int): Request =
        popularMangaRequest(page)

    override fun latestUpdatesParse(response: Response): MangasPage =
        popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val q = URLEncoder.encode(if (query.isBlank()) "manga" else query, "UTF-8")
        val start = (page - 1) * 10
        return GET("$baseUrl/search?q=$q&start=$start", headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = Jsoup.parse(response.body?.string() ?: "")
        val entries = document.select("div.g, div.tF2Cxc").mapNotNull { element ->
            val titleEl = element.selectFirst("h3") ?: return@mapNotNull null
            val linkEl = element.selectFirst("a[href]") ?: return@mapNotNull null
            val href = linkEl.attr("href")

            SManga.create().apply {
                title = titleEl.text()
                url = if (href.startsWith("http")) href.removePrefix(baseUrl) else href
                description = element.selectFirst("div.VwiC3b, div.IsZvec")?.text() ?: ""
                status = SManga.UNKNOWN
            }
        }
        return MangasPage(entries, hasNextPage = entries.isNotEmpty())
    }

    override fun mangaDetailsRequest(manga: SManga): Request =
        GET(if (manga.url.startsWith("http")) manga.url else baseUrl + manga.url, headers)

    override fun mangaDetailsParse(response: Response): SManga =
        SManga.create().apply {
            title = "Google Search Result"
            description = "Opened via Google WebView"
        }

    override fun chapterListRequest(manga: SManga): Request =
        mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> =
        listOf(
            SChapter.create().apply {
                name = "Web Page"
                url = "/"
                date_upload = System.currentTimeMillis()
            },
        )

    override fun pageListRequest(chapter: SChapter): Request =
        GET(baseUrl, headers)

    override fun pageListParse(response: Response): List<Page> =
        emptyList()

    override fun imageUrlParse(response: Response): String =
        throw UnsupportedOperationException("Not used")
}
