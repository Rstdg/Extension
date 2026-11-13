package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.extension.en.nhapi.NHentaiApi

class SourceManager {

    private val sourcesMap = mutableMapOf<Long, Source>()

    val nhentaiSource: NHentaiApi by lazy {
        NHentaiApi()
    }

    init {
        registerSource(nhentaiSource)
    }

    fun registerSource(source: Source) {
        sourcesMap[source.id] = source
    }

    fun get(id: Long): Source? = sourcesMap[id]

    fun getSources(): List<Source> = sourcesMap.values.toList()

    fun getNHentaiSource(): NHentaiApi = nhentaiSource

    companion object {
        @Volatile
        private var instance: SourceManager? = null

        fun getInstance(): SourceManager =
            instance ?: synchronized(this) {
                instance ?: SourceManager().also { instance = it }
            }
    }
}
