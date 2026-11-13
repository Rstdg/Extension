package eu.kanade.tachiyomi.source.model

sealed class Filter<T>(val name: String, var state: T) {
    open class Select<T>(name: String, val values: Array<T>, state: Int = 0) : Filter<Int>(name, state)
}
