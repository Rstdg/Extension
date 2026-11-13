package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

fun OkHttpClient.Builder.rateLimit(
    permits: Int = 1,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS
): OkHttpClient.Builder {
    return addInterceptor(RateLimitInterceptor(permits, period, unit))
}

class RateLimitInterceptor(
    private val permits: Int,
    private val period: Long,
    private val unit: TimeUnit
) : Interceptor {
    private val requestLog = ArrayList<Long>(permits)

    override fun intercept(chain: Interceptor.Chain): Response {
        val periodMs = unit.toMillis(period)

        synchronized(requestLog) {
            val now = System.currentTimeMillis()
            requestLog.removeAll { it <= now - periodMs }

            if (requestLog.size >= permits) {
                val sleepTime = periodMs - (now - requestLog.first())
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime)
                }
            }
            requestLog.add(System.currentTimeMillis())
        }

        return chain.proceed(chain.request())
    }
}
