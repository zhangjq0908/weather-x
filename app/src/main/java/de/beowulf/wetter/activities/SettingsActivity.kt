package de.beowulf.wetter.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.databinding.ActivitySettingsBinding
import de.beowulf.wetter.widget.WidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.function.Consumer
import javax.net.ssl.HttpsURLConnection

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val gf = GlobalFunctions()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(gf.getTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!gf.gradient(this)) {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            binding.root.setBackgroundColor(typedValue.data)
        }

        gf.initializeContext(this)

        val settings: SharedPreferences = getSharedPreferences("de.beowulf.wetter", 0)

        binding.CreatedBy.movementMethod = LinkMovementMethod.getInstance()
        binding.ReportError.movementMethod = LinkMovementMethod.getInstance()
        binding.OWM.movementMethod = LinkMovementMethod.getInstance()

        var latLon: String
        var city: String = settings.getString("city", "")!!
        var api: String = settings.getString("api", "")!!

        binding.City.setText(city)
        if (api != getString(R.string.standardKey))
            binding.Api.setText(api)

        binding.ApiText.setOnClickListener {
            val uriUrl: Uri = Uri.parse("https://home.openweathermap.org/api_keys")
            val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
            startActivity(launchBrowser)
        }

        binding.UISettings.setOnClickListener {
            changeSettings()
        }

        binding.GetLocation.setOnClickListener {
            getLocation()
        }

        binding.Submit.setOnClickListener {
            binding.loader.visibility = View.VISIBLE
            binding.mainContainer.visibility = View.GONE

            when {
                city != binding.City.text.toString() -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        var error = 3
                        val result: JSONArray = try {
                            city = binding.City.text.toString()
                            api = binding.Api.text.toString()
                            if (api == "")
                                api = getString(R.string.standardKey)

                            @Suppress("BlockingMethodInNonBlockingContext")
                            with(URL("https://api.openweathermap.org/geo/1.0/direct?q=$city&limit=10&appid=$api").openConnection() as HttpsURLConnection) {
                                sslSocketFactory = gf.getSocketFactory()
                                requestMethod = "GET"
                                error = when (responseCode) {
                                    429, 401 -> {
                                        1
                                    }
                                    404 -> {
                                        2
                                    }
                                    else -> {
                                        0
                                    }
                                }
                                JSONArray(BufferedReader(InputStreamReader(inputStream)).readText())
                            }
                        } catch (e: Exception) {
                            JSONArray("[]")
                        }
                        withContext(Dispatchers.Main) {
                            if (error == 0 && result.length() != 0) {
                                var i = 0
                                val cities = arrayOfNulls<String>(result.length())
                                while (i < result.length()) {
                                    if (result.getJSONObject(i).has("state")) {
                                        cities[i] = result.getJSONObject(i).getString("name") +
                                                ", " + result.getJSONObject(i).getString("state") +
                                                ", " + result.getJSONObject(i).getString("country")
                                    } else {
                                        cities[i] = result.getJSONObject(i).getString("name") +
                                                ", " + result.getJSONObject(i).getString("country")
                                    }
                                    i++
                                }

                                AlertDialog.Builder(
                                    ContextThemeWrapper(
                                        this@SettingsActivity,
                                        R.style.AlertDialog
                                    )
                                )
                                    .setTitle(R.string.choose_city)
                                    .setItems(cities) { _, selected ->
                                        city = result.getJSONObject(selected).getString("name") +
                                                ", " + result.getJSONObject(selected).getString("country")
                                        latLon =
                                            "lat=" + result.getJSONObject(selected).getString("lat") +
                                                    "&lon=" + result.getJSONObject(selected)
                                                .getString("lon")

                                        // Save all needed values
                                        settings.edit()
                                            .putString("latLon", latLon)
                                            .putString("city", city)
                                            .putString("api", api)
                                            .apply()

                                        startMain()
                                    }
                                    .setNegativeButton(R.string.abort) { dialog, _ ->
                                        dialog.cancel()
                                        binding.loader.visibility = View.GONE
                                        binding.mainContainer.visibility = View.VISIBLE
                                    }
                                    .setCancelable(false)
                                    .show()
                            } else {
                                binding.loader.visibility = View.GONE
                                binding.mainContainer.visibility = View.VISIBLE
                                displayError(error)
                            }
                        }
                    }
                }
                binding.City.text.toString() == "" -> {
                    binding.loader.visibility = View.GONE
                    binding.mainContainer.visibility = View.VISIBLE
                    Toast.makeText(
                        this@SettingsActivity,
                        R.string.error_2,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    //save api key, that it don't use old key
                    if (binding.Api.text.toString() != "") {
                        settings.edit()
                            .putString("api", binding.Api.text.toString())
                            .apply()
                    }
                    startMain()
                }
            }
        }
    }

    private fun startMain() {
        // Load data
        CoroutineScope(Dispatchers.IO).launch {
            var error = 3
            val result: String? = try {
                @Suppress("BlockingMethodInNonBlockingContext")
                with(URL(gf.url("normal", "")).openConnection() as HttpsURLConnection) {
                    sslSocketFactory = gf.getSocketFactory()
                    requestMethod = "GET"
                    error = when (responseCode) {
                        429, 401 -> {
                            1
                        }
                        404 -> {
                            2
                        }
                        else -> {
                            0
                        }
                    }
                    BufferedReader(InputStreamReader(inputStream)).readText()
                }
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                if (result != null) { // when loaded data isn't empty start the Main activity
                    gf.setResult(result)
                    gf.setInitialized(true)
                    val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                    startActivity(intent)
                    WidgetProvider().updateWidgets(applicationContext)
                    finish()
                } else { // else display error message
                    binding.loader.visibility = View.GONE
                    binding.mainContainer.visibility = View.VISIBLE
                    displayError(error)
                }
            }
        }
    }

    private fun changeSettings() {
        val settings: SharedPreferences = getSharedPreferences("de.beowulf.wetter", 0)
        val inflater = layoutInflater
        val alertLayout: View = inflater.inflate(R.layout.settings, null)
        val alert = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialog))
        //Get StringArrays
        val unitsTemp = resources.getStringArray(R.array.unitsTemp)
        val unitsSpeed = resources.getStringArray(R.array.unitsSpeed)
        val unitsDistance = resources.getStringArray(R.array.unitsDistance)
        val unitsPressure = resources.getStringArray(R.array.unitsPressure)
        val themes = resources.getStringArray(R.array.Themes)
        val langs = resources.getStringArray(R.array.lang)
        val langCodes = resources.getStringArray(R.array.lang_Code)
        //Load current settings
        var unitTemp: Int = settings.getInt("unitTemp", 1)
        var unitSpeed: Int = settings.getInt("unitSpeed", 0)
        var unitDistance: Int = settings.getInt("unitDistance", 0)
        var unitPressure: Int = settings.getInt("unitPressure", 0)
        var theme: Int = settings.getInt("Theme", 1) + 1
        var gradient = settings.getBoolean("Gradient", true)
        var colored = settings.getBoolean("colored_status", false)
        var timeFormat: Boolean = settings.getBoolean("24hTime", false)
        var autoLocDetect: Boolean = settings.getBoolean("locDetect", false)
        var lang: Int = langCodes.indexOf(settings.getString("lang", "system"))
        if (lang < 0) lang = 0
        //initialize elements
        val unitTempSpinner = alertLayout.findViewById<Spinner>(R.id.UnitTemp)
        val unitSpeedSpinner = alertLayout.findViewById<Spinner>(R.id.UnitSpeed)
        val unitDistanceSpinner = alertLayout.findViewById<Spinner>(R.id.UnitDistance)
        val unitPressureSpinner = alertLayout.findViewById<Spinner>(R.id.UnitPressure)
        val themeSpinner = alertLayout.findViewById<Spinner>(R.id.Theme)
        val gradientCheckbox = alertLayout.findViewById<CheckBox>(R.id.Gradient)
        val coloredCheckbox = alertLayout.findViewById<CheckBox>(R.id.Colored)
        val timeFormatCheckbox = alertLayout.findViewById<CheckBox>(R.id.TimeFormat)
        val autoLocDetectCheckbox = alertLayout.findViewById<CheckBox>(R.id.AutoLocDetect)
        val langSpinner = alertLayout.findViewById<Spinner>(R.id.Lang)
        //set current settings
        unitTempSpinner.adapter = ArrayAdapter(this, R.layout.spinner, unitsTemp)
        unitTempSpinner.setSelection(unitTemp)
        unitSpeedSpinner.adapter = ArrayAdapter(this, R.layout.spinner, unitsSpeed)
        unitSpeedSpinner.setSelection(unitSpeed)
        unitDistanceSpinner.adapter = ArrayAdapter(this, R.layout.spinner, unitsDistance)
        unitDistanceSpinner.setSelection(unitDistance)
        unitPressureSpinner.adapter = ArrayAdapter(this, R.layout.spinner, unitsPressure)
        unitPressureSpinner.setSelection(unitPressure)
        themeSpinner.adapter = ArrayAdapter(this, R.layout.spinner, themes)
        themeSpinner.setSelection(theme)
        gradientCheckbox.isChecked = gradient
        coloredCheckbox.isChecked = colored
        timeFormatCheckbox.isChecked = timeFormat
        autoLocDetectCheckbox.isChecked = autoLocDetect
        langSpinner.adapter = ArrayAdapter(this, R.layout.spinner, langs)
        langSpinner.setSelection(lang)
        //set AlertView
        alert.setView(alertLayout)
            .setCancelable(false)
            .setNegativeButton(R.string.abort) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(R.string.save) { dialog, _ ->
                unitTemp = unitTempSpinner.selectedItemPosition
                unitSpeed = unitSpeedSpinner.selectedItemPosition
                unitDistance = unitDistanceSpinner.selectedItemPosition
                unitPressure = unitPressureSpinner.selectedItemPosition
                colored = coloredCheckbox.isChecked
                timeFormat = timeFormatCheckbox.isChecked
                autoLocDetect = autoLocDetectCheckbox.isChecked
                settings.edit()
                    .putInt("unitTemp", unitTemp)
                    .putInt("unitSpeed", unitSpeed)
                    .putInt("unitDistance", unitDistance)
                    .putInt("unitPressure", unitPressure)
                    .putBoolean("colored_status", colored)
                    .putBoolean("24hTime", timeFormat)
                    .putBoolean("locDetect", autoLocDetect)
                    .apply()
                if (theme == themeSpinner.selectedItemPosition &&
                    gradient == gradientCheckbox.isChecked &&
                    lang == langSpinner.selectedItemPosition
                ) {
                    dialog.cancel()
                } else {
                    theme = themeSpinner.selectedItemPosition - 1
                    gradient = gradientCheckbox.isChecked
                    settings.edit()
                        .putInt("Theme", theme)
                        .putBoolean("Gradient", gradient)
                        .putString("lang", langCodes.getOrElse(langSpinner.selectedItemPosition) { "system" })
                        .apply()
                    finish()
                    startActivity(intent)
                }
            }
            .show()
    }

    private fun getLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialog))
        val layout: View = layoutInflater.inflate(R.layout.location_progress, null)
        builder.setView(layout)
        val pd: AlertDialog = builder.create()
        pd.setTitle(getString(R.string.detect_location))

        val locationConsumer = Consumer<Location?> { handleLocation(it, pd) }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            pd.show()
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, application.mainExecutor, locationConsumer)
                } else {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, { handleLocation(it, pd) }, application.mainLooper)
                }
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, application.mainExecutor, locationConsumer)
                } else {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, { handleLocation(it, pd) }, application.mainLooper)
                }
            } else {
                pd.cancel()
                Toast.makeText(this, getString(R.string.detect_location_activate), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.detect_location_activate), Toast.LENGTH_LONG).show()
        }
    }

    private fun handleLocation(location: Location?, pd: AlertDialog) {
        if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            val settings: SharedPreferences = getSharedPreferences("de.beowulf.wetter", 0)
            val api = settings.getString("api", getString(R.string.standardKey))!!

            //get City-Name
            CoroutineScope(Dispatchers.IO).launch {
                val result: JSONArray? = try {
                    with(withContext(Dispatchers.IO) {
                        URL("https://api.openweathermap.org/geo/1.0/reverse?lat=$lat&lon=$lon&appid=$api")
                            .openConnection()
                    } as HttpsURLConnection) {
                        sslSocketFactory = gf.getSocketFactory()
                        requestMethod = "GET"
                        JSONArray(BufferedReader(InputStreamReader(inputStream)).readText())
                    }
                } catch (e: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    pd.cancel()
                    if (result != null) {
                        val city = if (result.getJSONObject(0).has("state")) {
                            result.getJSONObject(0).getString("name") +
                                    ", " + result.getJSONObject(0).getString("state") +
                                    ", " + result.getJSONObject(0).getString("country")
                        } else {
                            result.getJSONObject(0).getString("name") +
                                    ", " + result.getJSONObject(0).getString("country")
                        }
                        binding.City.setText(city)
                        val latLon = "lat=$lat&lon=$lon"
                        // Save all needed values
                        settings.edit()
                            .putString("latLon", latLon)
                            .putString("city", city)
                            .putString("api", api)
                            .apply()
                    } else { // display error message, when result is empty
                        Toast.makeText(this@SettingsActivity, R.string.error_3, Toast.LENGTH_LONG)
                            .show()
                        binding.loader.visibility = View.GONE
                        binding.mainContainer.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            pd.cancel()
            Toast.makeText(this@SettingsActivity, R.string.detect_location_not_found, Toast.LENGTH_LONG).show()
        }
    }

    private fun displayError(error: Int) {
        when (error) {
            1 -> {
                Toast.makeText(
                    this@SettingsActivity,
                    R.string.error_1,
                    Toast.LENGTH_SHORT
                ).show()
            }
            0, 2 -> {
                Toast.makeText(
                    this@SettingsActivity,
                    R.string.error_2,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    this@SettingsActivity,
                    R.string.error_3,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }
        }
    }

    override fun onBackPressed() {
        val settings: SharedPreferences = getSharedPreferences("de.beowulf.wetter", 0)
        if (binding.City.text.toString() == settings.getString("city", "")) {
            if (binding.Api.text.toString() != "") {
                settings.edit()
                    .putString("api", binding.Api.text.toString())
                    .apply()
            }
            val intent = Intent(this@SettingsActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            binding.Submit.callOnClick()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GlobalFunctions().getLangCon(newBase))
    }
}