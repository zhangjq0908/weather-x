package de.beowulf.wetter.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.databinding.ActivityLaunchBinding
import de.beowulf.wetter.widget.WidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class LaunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaunchBinding

    private val gf = GlobalFunctions()
    private lateinit var settings: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(GlobalFunctions().getTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!gf.gradient(this)) {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            binding.root.setBackgroundColor(typedValue.data)
        }

        gf.initializeContext(this)

        settings = getSharedPreferences("de.beowulf.wetter", 0)

        if (settings.getBoolean("locDetect", false))
            getLocation()

        if (gf.getInitialized()) {
            CoroutineScope(Dispatchers.IO).launch {
                val result: String? = try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    with(URL(gf.url("normal", "")).openConnection() as HttpsURLConnection) {
                        sslSocketFactory = gf.getSocketFactory()
                        requestMethod = "GET"
                        BufferedReader(InputStreamReader(inputStream)).readText()
                    }
                } catch (e: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        gf.setResult(result)
                        gf.setInitialized(true)
                    } else {
                        Toast.makeText(this@LaunchActivity, R.string.error_occurred, Toast.LENGTH_LONG).show()
                    }
                    val intent = Intent(this@LaunchActivity, MainActivity::class.java)
                    startActivity(intent)
                    WidgetProvider().updateWidgets(applicationContext)
                    finish()
                }
            }
        } else {
            val intent = Intent(this@LaunchActivity, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                try {
                    locationManager.removeUpdates(this)
                } catch (_: SecurityException) { }

                val lat = location.latitude
                val lon = location.longitude
                val api = settings.getString("api", getString(R.string.standardKey))!!

                //get City-Name
                CoroutineScope(Dispatchers.IO).launch {
                    val result: JSONArray? = try {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        with(URL("https://api.openweathermap.org/geo/1.0/reverse?lat=$lat&lon=$lon&appid=$api")
                            .openConnection() as HttpsURLConnection) {
                            sslSocketFactory = gf.getSocketFactory()
                            requestMethod = "GET"
                            JSONArray(BufferedReader(InputStreamReader(inputStream)).readText())
                        }
                    } catch (e: Exception) {
                        null
                    }
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            val city = if (result.getJSONObject(0).has("state")) {
                                result.getJSONObject(0).getString("name") +
                                        ", " + result.getJSONObject(0).getString("state") +
                                        ", " + result.getJSONObject(0).getString("country")
                            } else {
                                result.getJSONObject(0).getString("name") +
                                        ", " + result.getJSONObject(0).getString("country")
                            }
                            val latLon = "lat=$lat&lon=$lon"

                            if (settings.getString("city", "") != city) {
                                Toast.makeText(
                                    this@LaunchActivity,
                                    getString(R.string.detect_location_new_location),
                                    Toast.LENGTH_LONG
                                ).show()
                                // Save all needed values
                                settings.edit()
                                    .putString("latLon", latLon)
                                    .putString("city", city)
                                    .putString("api", api)
                                    .apply()
                                val intent = Intent(applicationContext, LaunchActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this@LaunchActivity, getString(R.string.detect_location_permission_not_granted), Toast.LENGTH_LONG).show()
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GlobalFunctions().getLangCon(newBase))
    }
}