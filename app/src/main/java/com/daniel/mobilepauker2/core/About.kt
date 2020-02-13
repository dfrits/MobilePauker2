package com.daniel.mobilepauker2.core

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.BuildConfig
import com.daniel.mobilepauker2.R
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Daniel on 16.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class About : AppCompatActivity() {

    companion object {
        private val fileNameMap: MutableMap<Locale, String> = HashMap()

        init {
            fileNameMap[Locale.ENGLISH] = "file:///android_asset/instructions_en.html"
            fileNameMap[Locale.GERMANY] = "file:///android_asset/instructions_de.html"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about)
        val webView = findViewById<WebView>(R.id.tAboutText)
        var fileName =
            fileNameMap[Locale.getDefault()]
        fileName =
            fileName ?: fileNameMap[Locale.ENGLISH]
        webView.loadUrl(fileName)
        webView.setBackgroundColor(getColor(R.color.defaultBackground))
        val version = "Version: " + BuildConfig.VERSION_NAME
        (findViewById<View>(R.id.tVersion) as TextView).text = version
    }
}