package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList

interface Source {
    val id: Long get() = name.hashCode().toLong()
    val name: String
    val lang: String

    fun getFilterList(): FilterList = FilterList()
}
