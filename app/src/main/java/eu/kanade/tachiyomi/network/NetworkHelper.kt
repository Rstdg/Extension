package eu.kanade.tachiyomi.network

import okhttp3.OkHttpClient

class NetworkHelper {
    val client: OkHttpClient = OkHttpClient()
    val cloudflareClient: OkHttpClient = client.newBuilder().build()
}
