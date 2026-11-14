package eu.kanade.tachiyomi.extension.en.nhentai

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log

/** Opens nhentai.net/g/<id>/ links from the browser straight into the app. */
class NHentaiUrlActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val segments = intent?.data?.pathSegments
        val id = segments?.firstOrNull { it.all(Char::isDigit) }
        if (id != null) {
            val mainIntent = Intent().apply {
                action = "eu.kanade.tachiyomi.SEARCH"
                putExtra("query", "id:$id")
                putExtra("filter", packageName)
            }
            try {
                startActivity(mainIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e("NHentaiUrlActivity", e.toString())
            }
        } else {
            Log.e("NHentaiUrlActivity", "could not parse uri from intent $intent")
        }
        finish()
    }
}
