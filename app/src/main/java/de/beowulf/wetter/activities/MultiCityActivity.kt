package de.beowulf.wetter.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.adapter.MyListDayCitiesAdapter
import de.beowulf.wetter.databinding.WeatherForecastBinding
import de.beowulf.wetter.widget.WidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

class MultiCityActivity : AppCompatActivity() {

    private lateinit var binding: WeatherForecastBinding

    private val gf = GlobalFunctions()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(gf.getTheme(this))
        super.onCreate(savedInstanceState)
        binding = WeatherForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val typedValue = TypedValue()
        if (gf.gradient(this)) {
            theme.resolveAttribute(R.attr.bg_gradient, typedValue, true)
            binding.root.setBackgroundResource(typedValue.resourceId)
        } else {
            theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            binding.root.setBackgroundColor(typedValue.data)
        }

        gf.initializeContext(this)

        binding.Add.visibility = View.VISIBLE

        loadData()

        binding.Add.setOnClickListener {
            binding.Add.visibility = View.GONE
            binding.AddCity.visibility = View.VISIBLE
        }

        binding.Save.setOnClickListener {
            val city = binding.City.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                var error = 3
                val result: JSONArray = try {
                    val settings: SharedPreferences = this@MultiCityActivity.getSharedPreferences("de.beowulf.wetter", 0)
                    val api = settings.getString("api", "")

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
                        val posCity = arrayOfNulls<String>(result.length())
                        while (i < result.length())
                        {
                            if (result.getJSONObject(i).has("state")) {
                                posCity[i] = result.getJSONObject(i).getString("name") +
                                        ", " + result.getJSONObject(i).getString("state") +
                                        ", " + result.getJSONObject(i).getString("country")
                            } else {
                                posCity[i] = result.getJSONObject(i).getString("name") +
                                        ", " + result.getJSONObject(i).getString("country")
                            }
                            i++
                        }

                        AlertDialog.Builder(
                            ContextThemeWrapper(
                                this@MultiCityActivity,
                                R.style.AlertDialog
                            )
                        )
                            .setTitle(R.string.choose_city)
                            .setItems(posCity) { _, selected ->
                                val selectedCity = result.getJSONObject(selected).getString("name") +
                                        ", " + result.getJSONObject(selected).getString("country")
                                val latLon = "lat=" + result.getJSONObject(selected).getString("lat") +
                                        "&lon=" + result.getJSONObject(selected).getString("lon") +
                                        ";${selectedCity}"

                                val cities = gf.getCities().toMutableList()
                                cities.add(latLon)
                                gf.setCities(cities.toTypedArray())
                                binding.City.setText("")
                                binding.Add.visibility = View.VISIBLE
                                binding.AddCity.visibility = View.GONE
                                loadData()
                            }
                            .show()
                    } else {
                        when (error) {
                            1 -> {
                                Toast.makeText(this@MultiCityActivity, R.string.error_1, Toast.LENGTH_SHORT).show()
                            }
                            0, 2 -> {
                                Toast.makeText(this@MultiCityActivity, R.string.error_2, Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this@MultiCityActivity, R.string.error_3, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        binding.ListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view, _, _ ->
                val moreInfo = view.findViewById<LinearLayout>(R.id.dayMoreInfo)
                if (moreInfo.visibility == View.GONE) {
                    moreInfo.visibility = View.VISIBLE
                } else {
                    moreInfo.visibility = View.GONE
                }
            }

        binding.ListView.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, view, ID, _ ->
                val cityName = view.findViewById<TextView>(R.id.DayDate)
                val city: String = cityName.text.dropLast(1).toString()
                val settings: SharedPreferences = this.getSharedPreferences("de.beowulf.wetter", 0)
                val options = arrayOf(getString(R.string.removeCity, city), getString(R.string.switchCity, city, settings.getString("city", "")))

                AlertDialog.Builder(ContextThemeWrapper(this@MultiCityActivity, R.style.AlertDialog))
                    .setTitle(getString(R.string.options))
                    .setItems(options) { _, id ->
                        when (id) {
                            0 -> {
                                val cities = gf.getCities().toMutableList()
                                cities.removeAt(ID)
                                gf.setCities(cities.toTypedArray())
                                loadData()
                            }
                            1 -> {
                                switchCity(ID, city)
                            }
                        }
                    }
                    .setNegativeButton(R.string.abort) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()
                true
            }
    }

    private fun loadData() {
        val cities = gf.getCities()

        val cityName = arrayOfNulls<String>(cities.size)
        val statusCity = arrayOfNulls<Int>(cities.size)
        val statusCityTint = arrayOfNulls<Int>(cities.size)
        val statusCityText = arrayOfNulls<String>(cities.size)
        val temp = arrayOfNulls<String>(cities.size)
        val tempFeeled = arrayOfNulls<String>(cities.size)
        val sunrise = arrayOfNulls<String>(cities.size)
        val sunset = arrayOfNulls<String>(cities.size)
        val wind = arrayOfNulls<String>(cities.size)
        val pressure = arrayOfNulls<String>(cities.size)
        val humidity = arrayOfNulls<String>(cities.size)
        val rainSnow = arrayOfNulls<String>(cities.size)
        val rainSnowType = arrayOfNulls<String>(cities.size)
        val windGust = arrayOfNulls<String>(cities.size)

        for (city in cities) {

            CoroutineScope(Dispatchers.IO).launch {
                val result: JSONObject? = try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    with(URL(gf.url("cities", city.split(";")[0])).openConnection() as HttpsURLConnection) {
                        sslSocketFactory = gf.getSocketFactory()
                        requestMethod = "GET"
                        JSONObject(BufferedReader(InputStreamReader(inputStream)).readText())
                    }
                } catch (e: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    if (result == null) {
                        Toast.makeText(
                            this@MultiCityActivity,
                            R.string.error_3,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val i: Int = cities.indexOf(city)
                        val status = result.getJSONArray("weather").getJSONObject(0)
                        val main = result.getJSONObject("main")

                        cityName[i] = city.split(";")[1] + " "
                        statusCity[i] = gf.icon(status.getString("icon"))
                        statusCityTint[i] = gf.iconTint(status.getString("icon"))
                        statusCityText[i] = status.getString("description")
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                        temp[i] = gf.convertTemp(main.getDouble("temp"))
                        tempFeeled[i] = gf.convertTemp(main.getDouble("feels_like"))
                        sunrise[i] =
                            SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
                                Date(
                                    result.getJSONObject("sys").getLong("sunrise") * 1000
                                )
                            )
                        sunset[i] =
                            SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
                                Date(
                                    result.getJSONObject("sys").getLong("sunset") * 1000
                                )
                            )
                        wind[i] = gf.convertSpeed(result.getJSONObject("wind").getDouble("speed")) +
                                " (${gf.degToCompass(result.getJSONObject("wind").getInt("deg"))})"
                        if (result.getJSONObject("wind").has("gust")) {
                            windGust[i] =
                                gf.convertSpeed(result.getJSONObject("wind").getDouble("gust"))
                        }
                        pressure[i] = gf.convertPressure(main.getDouble("pressure"))
                        humidity[i] = main.getString("humidity") + "%"
                        when {
                            result.has("rain") -> {
                                rainSnow[i] =
                                    gf.convertRain(result.getJSONObject("rain").getDouble("1h"))
                                rainSnowType[i] = "rain"
                            }
                            result.has("snow") -> {
                                rainSnow[i] =
                                    gf.convertRain(result.getJSONObject("snow").getDouble("1h"))
                                rainSnowType[i] = "snow"
                            }
                            else -> {
                                rainSnow[i] = gf.convertRain(0.0)
                                rainSnowType[i] = "rain"
                            }
                        }

                        val myListAdapter = MyListDayCitiesAdapter(
                            this@MultiCityActivity,
                            cityName,
                            null,
                            statusCity,
                            statusCityTint,
                            statusCityText,
                            temp,
                            tempFeeled,
                            sunrise,
                            sunset,
                            wind,
                            pressure,
                            humidity,
                            rainSnow,
                            rainSnowType,
                            null,
                            null,
                            windGust
                        )
                        binding.ListView.adapter = myListAdapter
                    }
                }
            }
        }
        val myListAdapter = MyListDayCitiesAdapter(
            this@MultiCityActivity,
            cityName,
            null,
            statusCity,
            statusCityTint,
            statusCityText,
            temp,
            tempFeeled,
            sunrise,
            sunset,
            wind,
            pressure,
            humidity,
            rainSnow,
            rainSnowType,
            null,
            null,
            windGust
        )
        binding.ListView.adapter = myListAdapter
    }

    private fun switchCity(id: Int, city: String) {
        val settings: SharedPreferences = this.getSharedPreferences("de.beowulf.wetter", 0)

        //swap city in list
        val cities = gf.getCities().toMutableList()
        var listCity = cities[id].split(";")[0]
        val cityOld = settings.getString("city", "")!!
        cities[id] = settings.getString("latLon", "")!! + ";$cityOld"
        settings.edit()
            .putString("latLon", listCity)
            .putString("city", city)
            .apply()
        gf.setCities(cities.toTypedArray())

        // Load new data
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
                if (result != null) { // when loaded data isn't empty, or old data is available, start the Main activity
                    gf.setResult(result)

                    val intent = Intent(this@MultiCityActivity, MainActivity::class.java)
                    startActivity(intent)
                    WidgetProvider().updateWidgets(applicationContext)
                    finish()
                } else { // else display error message
                    Toast.makeText(this@MultiCityActivity, R.string.error_3, Toast.LENGTH_LONG).show()
                    //revert swap city in list
                    listCity = cities[id].split(";")[0]
                    cities[id] = settings.getString("latLon", "")!! + ";${settings.getString("city", "")}"
                    settings.edit()
                        .putString("latLon", listCity)
                        .putString("city", cityOld)
                        .apply()
                    gf.setCities(cities.toTypedArray())
                }
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
