package de.beowulf.wetter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.databinding.ActivityMapBinding
import java.text.SimpleDateFormat
import java.util.*

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding

    private val gf = GlobalFunctions()

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(gf.getTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gf.initializeContext(this)

        binding.MapView.settings.javaScriptEnabled = true
        binding.MapView.addJavascriptInterface(JsWebInterface(this), "Android")
        binding.MapView.loadUrl(gf.url("Map", ""))
        binding.MapView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                    setOverlay(binding.navBar.selectedItemId)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                return if (url != null && url.startsWith("https://")) {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } else false
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return if (request.url.toString().startsWith("https://")) {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                } else false
            }
        }

        binding.navBar.setOnItemSelectedListener { item ->
            val i = item.itemId
            setOverlay(i)
            true
        }
    }

    private fun setOverlay(item: Int) {
        when (item) {
            R.id.map_temp -> binding.MapView.loadUrl(
                ("javascript:map.removeLayer(cloudsLayer);map.removeLayer(rainLayer);map.removeLayer(windLayer);hideRainRadar();"
                            + "map.addLayer(tempLayer);")
            )
            R.id.map_rain_clouds -> binding.MapView.loadUrl(
                ("javascript:map.removeLayer(tempLayer);map.removeLayer(windLayer);hideRainRadar();"
                            + "map.addLayer(cloudsLayer);map.addLayer(rainLayer);")
            )
            R.id.map_wind -> binding.MapView.loadUrl(
                ("javascript:map.removeLayer(tempLayer);map.removeLayer(cloudsLayer);map.removeLayer(rainLayer);hideRainRadar();"
                            + "map.addLayer(windLayer);")
            )
            R.id.map_rainRadar -> binding.MapView.loadUrl(
                ("javascript:map.removeLayer(tempLayer);map.removeLayer(cloudsLayer);map.removeLayer(rainLayer);map.removeLayer(windLayer);"
                        + "showRainRadar();")
            )
        }
    }

    class JsWebInterface(val context: Context) {
        @JavascriptInterface
        fun date(time: Long): String {
            val timeFormat = if (context.getSharedPreferences("de.beowulf.wetter", 0).
                getBoolean("24hTime", false)) {
                    context.resources.getString(R.string.time24)
                } else {
                    context.resources.getString(R.string.time12)
                }
            return SimpleDateFormat(
                "${context.getString(R.string.date)} $timeFormat",
                Locale.ROOT
            ).format(Date(time))
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}