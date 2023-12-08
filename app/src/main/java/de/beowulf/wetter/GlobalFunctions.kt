package de.beowulf.wetter

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.util.TypedValue
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

class GlobalFunctions {

    private lateinit var settings: SharedPreferences
    private lateinit var context: Context
    var locale: Locale? = null

    fun initializeContext(con: Context) {
        settings = con.getSharedPreferences("de.beowulf.wetter", 0)
        context = con
    }

    fun degToCompass(num: Int): String {
        val arr: Array<String> = context.resources.getStringArray(R.array.Degree)
        val value: Int = floor((num / 22.5) + 0.5).toInt()
        return arr[(value % 16)]
    }

    fun result(): JSONObject {
        return JSONObject(settings.getString("result", "").toString())
    }

    fun setResult(result: String) {
        val editor: SharedPreferences.Editor = settings.edit()
        editor.putString("result", result)
        editor.putBoolean("initialized", true)
        editor.apply()
    }

    private fun getLanguage(system: String = Locale.getDefault().language): String {
        val lang = kotlin.runCatching {
            settings.getString("lang", "system") ?: "system"
        }.getOrDefault("system")
        return if (lang == "system") {
            system
        } else {
            lang
        }
    }

    fun url(type: String, city: String): String {
        val api = settings.getString("api", "")
        val latLon = settings.getString("latLon", "")!!
        val night = (getTheme(context) == R.style.DarkTheme)

        return when (type) {
            "cities" -> {
                "https://api.openweathermap.org/data/2.5/weather?$city&appid=$api&lang=${getLanguage()}"
            }
            "Map" -> {
                "file:///android_asset/map.html?$latLon&appid=$api&zoom=10&night=$night"
            }
            else -> {
                "https://api.openweathermap.org/data/2.5/onecall?$latLon&exclude=minutely&appid=$api&lang=${getLanguage()}"
            }
        }
    }

    fun icon(iconName: String): Int {
        val icon =
            if (iconName.startsWith("04")) {
                "status${iconName.dropLast(1)}"
            } else {
                "status$iconName"
            }
        return context.resources.getIdentifier(icon, "drawable", context.packageName)
    }

    fun iconTint(iconName: String): Int {
        if (!settings.getBoolean("colored_status", false)) {
            return getThemeColor(R.attr.frontColor)
        }
        return when (iconName.dropLast(1).toInt()) {
            1 -> getThemeColor(R.attr.sun)
            2,50 -> getThemeColor(R.attr.clouds_gust)
            3 -> getThemeColor(R.attr.clouds)
            4 -> getThemeColor(R.attr.broken_clouds)
            9 -> getThemeColor(R.attr.few_rain)
            10 -> getThemeColor(R.attr.rain)
            11 -> getThemeColor(R.attr.storm)
            13 -> getThemeColor(R.attr.snow)
            else -> getThemeColor(R.attr.frontColor)
        }
    }

    private fun getThemeColor(color: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(color, typedValue, true)
        return typedValue.data
    }

    fun getCities(): Array<String> {
        return settings.getStringSet("cityList", emptySet())?.toTypedArray() ?: emptyArray()
    }

    fun setCities(cities: Array<String>) {
        val editor: SharedPreferences.Editor = settings.edit()
        editor.putStringSet("cityList", cities.toSet())
        editor.apply()
    }

    fun getInitialized(): Boolean {
        return settings.getBoolean("init", false)
    }

    fun setInitialized(bool: Boolean) {
        settings.edit()
            .putBoolean("init", bool)
            .apply()
    }

    fun unitTemp(): String {
        return " (${context.resources.getStringArray(R.array.unitsTemp)[settings.getInt("unitTemp", 1)]})"
    }

    fun unitRain(): String {
        return context.resources.getStringArray(R.array.unitsDistance)[settings.getInt("unitDistance", 0)]
    }

    fun graphTemp(temp: Double): Int {
        return when (settings.getInt("unitTemp", 1)) {
             1 -> { //°C
                (temp - 273.15).roundToInt()
            }
            2 -> {//°F
                (temp * 9 / 5 - 459.67).roundToInt()
            }
            else -> { //K
                temp.roundToInt()
            }
        }
    }

    fun graphRain(rain: Double): Double {
        return when {
            rain == 0.0 -> {
                0.00
            }
            settings.getInt("unitDistance", 0) == 1 -> { //in
                ((rain / 25.4) * 100).roundToInt() / 100.0
            }
            else -> { //mm
                (rain * 100).roundToInt() / 100.0
            }
        }
    }

    fun getTime(): String {
        return if (settings.getBoolean("24hTime", false)) {
            context.resources.getString(R.string.time24)
        } else {
            context.resources.getString(R.string.time12)
        }
    }

    fun convertTemp(temp: Double): String {
        return graphTemp(temp).toString() +
                context.resources.getStringArray(R.array.unitsTemp)[settings.getInt("unitTemp", 1)]
    }

    fun convertSpeed(speed: Double): String {
        return when (settings.getInt("unitSpeed", 0)) {
             1 -> { //km/h
                String.format("%.2f", (speed * 3.6))
            }
            2 -> { //mph
                String.format("%.2f", (speed * 2.23693629205))
            }
            3 -> { //bft
                (speed / 0.8360).pow(2.0/3.0).roundToInt().toString()
            }
            4 -> { //kn
                String.format("%.2f", (speed * 1.943844))
            }
            else -> { //m/s
                String.format("%.2f", speed)
            }
        } + context.resources.getStringArray(R.array.unitsSpeed)[settings.getInt("unitSpeed", 0)]
    }

    fun convertRain(rain: Double): String {
        return String.format("%.2f", graphRain(rain)) +
                context.resources.getStringArray(R.array.unitsDistance)[settings.getInt("unitDistance", 0)]
    }

    fun convertPressure(pressure: Double): String {
        return when (settings.getInt("unitPressure", 0)) {
            1 -> { //kPa
                (pressure / 10).roundToInt().toString()
            }
            2 -> { //mm Hg
                "%.2f".format(pressure / 1.33322387415)
            }
            3 -> { //in Hg
                "%.2f".format(pressure / 33.863886666667)
            }
            4 -> { //psi
                (pressure / 68.9475728).roundToInt().toString()
            }
            5 -> { //atm
                (pressure / 1013.25).roundToInt().toString()
            }
            else -> { //hPa
                pressure.roundToInt().toString()
            }
        } + context.resources.getStringArray(R.array.unitsPressure)[settings.getInt("unitPressure", 0)]
    }

    fun getTheme(con: Context): Int {
        context = con
        settings = con.getSharedPreferences("de.beowulf.wetter", 0)
        return when (settings.getInt("Theme", 1)) {
            -1 -> {
                val mode = context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)
                if (mode == Configuration.UI_MODE_NIGHT_YES)
                    R.style.DarkTheme
                else
                    R.style.LightTheme
            }
            1 -> {
                R.style.DarkTheme
            }
            2 -> {
                R.style.RedTheme
            }
            3 -> {
                R.style.SandTheme
            }
            4 -> {
                R.style.BlueTheme
            }
            5 -> {
                R.style.BlackTheme
            }
            else -> {
                R.style.LightTheme
            }
        }
    }

    fun gradient(con: Context): Boolean {
        settings = con.getSharedPreferences("de.beowulf.wetter", 0)
        return settings.getBoolean("Gradient", true)
    }

    fun widgetSettings(id: Int): String? {
        return settings.getString("widget_$id", null)
    }

    fun getSocketFactory(): SSLSocketFactory? {
        val cf: CertificateFactory?
        try {
            // Load CAs from an InputStream
            cf = CertificateFactory.getInstance("X.509")
            val caInput: InputStream = BufferedInputStream(
                context.assets.open("openweathermap.crt")
            )
            val ca: Certificate
            caInput.use { caIn ->
                ca = cf.generateCertificate(caIn)
            }

            // Create a KeyStore containing our trusted CAs
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ca", ca)

            // Create a TrustManager that trusts the CAs in our KeyStore
            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
            tmf.init(keyStore)

            // Create an SSLContext that uses our TrustManager
            val context = SSLContext.getInstance("TLS")
            context.init(null, tmf.trustManagers, null)
            return context.socketFactory
        } catch (_: java.lang.Exception) { }
        return null
    }

    @Suppress("DEPRECATION")
    fun getLangCon(con: Context): Context {
        context = con
        settings = con.getSharedPreferences("de.beowulf.wetter", 0)
        val lang = getLanguage(system = "system").split("_")
        if (lang[0] == "system") {
            return con
        } else {
            val conf = context.resources.configuration
            //Check if lang is set to Serbian (Latin)
            if (lang[0] == "sr" && locale == null) {
                for (loc in Locale.getAvailableLocales()) {
                    if (loc.displayName.equals("Serbian (Latin)")) {
                        locale = loc
                        break
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (locale != null)
                    conf.setLocale(locale)
                else
                    conf.setLocale(Locale(lang[0], if (lang.size > 1) lang[1] else ""))
                context = context.createConfigurationContext(conf)
            } else {
                if (locale != null)
                    conf.locale = locale
                else
                    conf.locale = Locale(lang[0], if (lang.size > 1) lang[1] else "")
                context.resources.updateConfiguration(conf, context.resources.displayMetrics)
            }
            return context
        }
    }
}