package eu.kanade.tachiyomi.network

import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request

fun GET(
    url: String,
    headers: Headers = Headers.Builder().build(),
    cache: CacheControl? = null
): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply {
            if (cache != null) {
                cacheControl(cache)
            }
        }
        .build()
}

fun GET(
    url: HttpUrl,
    headers: Headers = Headers.Builder().build(),
    cache: CacheControl? = null
): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply {
            if (cache != null) {
                cacheControl(cache)
            }
        }
        .build()
}
