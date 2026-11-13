package com.rsdgt.mihon.data.repository

import eu.kanade.tachiyomi.extension.en.nhapi.NHentaiApi
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MangaRepository(
    private val sourceManager: SourceManager = SourceManager.getInstance()
) {
    val source: NHentaiApi get() = sourceManager.getNHentaiSource()

    suspend fun getPopularManga(page: Int): Result<MangasPage> = withContext(Dispatchers.IO) {
        runCatching {
            val request = source.popularMangaRequest(page)
            source.client.newCall(request).execute().use { response ->
                source.popularMangaParse(response)
            }
        }
    }

    suspend fun getLatestManga(page: Int): Result<MangasPage> = withContext(Dispatchers.IO) {
        runCatching {
            val request = source.latestUpdatesRequest(page)
            source.client.newCall(request).execute().use { response ->
                source.latestUpdatesParse(response)
            }
        }
    }

    suspend fun searchManga(page: Int, query: String, sortIndex: Int): Result<MangasPage> = withContext(Dispatchers.IO) {
        runCatching {
            val filterList = source.getFilterList(sortIndex)
            val request = source.searchMangaRequest(page, query, filterList)
            source.client.newCall(request).execute().use { response ->
                source.searchMangaParse(response)
            }
        }
    }

    suspend fun getMangaDetails(manga: SManga): Result<SManga> = withContext(Dispatchers.IO) {
        runCatching {
            val request = source.mangaDetailsRequest(manga)
            source.client.newCall(request).execute().use { response ->
                source.mangaDetailsParse(response)
            }
        }
    }

    suspend fun getChapterList(manga: SManga): Result<List<SChapter>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = source.chapterListRequest(manga)
            source.client.newCall(request).execute().use { response ->
                source.chapterListParse(response)
            }
        }
    }

    suspend fun getPageList(chapter: SChapter): Result<List<Page>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = source.pageListRequest(chapter)
            source.client.newCall(request).execute().use { response ->
                source.pageListParse(response)
            }
        }
    }
}
