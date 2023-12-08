package de.beowulf.wetter.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.beowulf.wetter.*
import de.beowulf.wetter.activities.*
import de.beowulf.wetter.databinding.FragmentMainBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    private var city: String? = ""
    private val gf = GlobalFunctions()

    private lateinit var alert: String

    private val handlerThread = HandlerThread("UpdateThread")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        val view: View = binding.root

        gf.initializeContext(requireContext())

        val settings: SharedPreferences = requireContext().getSharedPreferences("de.beowulf.wetter", 0)

        city = settings.getString("city", "")

        binding.DaytimeText.isSelected = true
        binding.Daytime.isSelected = true
        binding.HumidityText.isSelected = true

        if (binding.belowInfromations != null) {
            binding.overviewContainer.setPadding(0, 0, 0, binding.belowInfromations!!.height + 50)
        }

        // display data
        setData()

        binding.Address.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            val intent = Intent(activity, LaunchActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        binding.Map.setOnClickListener {
            val intent = Intent(activity, MapActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        binding.MultiCities.setOnClickListener {
            val intent = Intent(activity, MultiCityActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        binding.weatherAlerts.setOnClickListener {
            if (this::alert.isInitialized) {
                val intent = Intent(activity, WeatherAlertActivity::class.java)
                intent.putExtra("alert", alert)
                startActivity(intent)
                activity?.finish()
            }
        }

        //Update UI and weather data after 30min, when app is open
        handlerThread.start()
        Handler(handlerThread.looper).postDelayed({
            if (activity != null) {
                val intent = Intent(activity, LaunchActivity::class.java)
                startActivity(intent)
                activity?.finish()
            }
        }, 1800000)

        return view
    }

    private fun setData() {
        val jsonObj = gf.result()
        val current: JSONObject = jsonObj.getJSONObject("current")
        val currentWeather: JSONObject =
            current.getJSONArray("weather").getJSONObject(0)
        val daily: JSONObject =
            jsonObj.getJSONArray("daily").getJSONObject(0).getJSONObject("temp")

        val updatedAt: Long = current.getLong("dt")
        val sdf = SimpleDateFormat(getString(R.string.date), Locale.getDefault())
        val updatedAtText: String = getString(R.string.updated) + " " +
                if (sdf.format(System.currentTimeMillis()).equals(sdf.format(updatedAt * 1000))) {
                    SimpleDateFormat(
                        gf.getTime(),
                        Locale.ROOT
                    ).format(Date(updatedAt * 1000))
                } else {
                    SimpleDateFormat(
                        "${getString(R.string.date)} ${gf.getTime()}",
                        Locale.ROOT
                    ).format(Date(updatedAt * 1000))
                }
        val temp: String = gf.convertTemp(current.getDouble("temp"))
        val tempFeels: String =
            getString(R.string.feels) + " " + gf.convertTemp(current.getDouble("feels_like"))
        val tempMinCut: String =
            "${getString(R.string.Min)}: " + gf.convertTemp(daily.getDouble("min"))
        val tempMaxCut: String =
            "${getString(R.string.Max)}: " + gf.convertTemp(daily.getDouble("max"))
        val pressure: String = gf.convertPressure(current.getDouble("pressure"))
        val humidity: String = current.getString("humidity") + "%"
        val rainSnowPop: Double =
            jsonObj.getJSONArray("hourly").getJSONObject(0).getDouble("pop") * 100
        when {
            current.has("rain") -> {
                val rain = gf.convertRain(current.getJSONObject("rain").getDouble("1h"))
                binding.Rain.text = rain
                val rainText: String =
                    getString(R.string.rain) + " (${rainSnowPop.toString().split(".")[0]}%)"
                binding.RainText.text = rainText
                binding.RainLL.visibility = View.VISIBLE
                binding.SnowLL.visibility = View.GONE
            }
            current.has("snow") -> {
                val snow = gf.convertRain(current.getJSONObject("snow").getDouble("1h"))
                binding.Snow.text = snow
                val snowText =
                    getString(R.string.snow) + " (${rainSnowPop.toString().split(".")[0]}%)"
                binding.SnowText.text = snowText
                binding.SnowLL.visibility = View.VISIBLE
                binding.RainLL.visibility = View.GONE
            }
            else -> {
                val rain = gf.convertRain(0.0)
                binding.Rain.text = rain
                val rainText =
                    getString(R.string.rain) + " (${rainSnowPop.toString().split(".")[0]}%)"
                binding.RainText.text = rainText
                binding.RainLL.visibility = View.VISIBLE
                binding.SnowLL.visibility = View.GONE
            }
        }

        val sunrise: Long = current.getLong("sunrise")
        val sunset: Long = current.getLong("sunset")
        val sunriseSunset = "${SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
            Date(
                sunrise * 1000
            )
        )} / ${SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
            Date(
                sunset * 1000
            )
        )}"
        val uvIndex = current.getString("uvi")
        val uvColor = when (current.getInt("uvi")) {
            0, 1, 2 -> {
                Color.GREEN
            }
            3, 4, 5 -> {
                Color.YELLOW
            }
            6, 7 -> {
                Color.rgb(255, 87, 34)
            }
            8, 9, 10 -> {
                Color.RED
            }
            else -> {
                Color.MAGENTA
            }
        }
        val windSpeedDouble: Double = current.getDouble("wind_speed")
        val windSpeed = gf.convertSpeed(windSpeedDouble)
        val windDegree: Int = current.getInt("wind_deg")
        val weatherDescription: String = currentWeather.getString("description")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val icon: Int = gf.icon(currentWeather.getString("icon"))
        val iconTint: Int = gf.iconTint(currentWeather.getString("icon"))

        if (jsonObj.has("alerts")) {
            alert = jsonObj.getJSONArray("alerts").toString()
            binding.weatherAlerts.visibility = View.VISIBLE
        }

        /* Populating extracted data into our views */
        binding.Address.text = city
        binding.UpdatedAt.text = updatedAtText
        binding.ImgStatus.setImageResource(icon)
        binding.ImgStatus.setColorFilter(iconTint)
        binding.Status.text =
            weatherDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        binding.Temp.text = temp
        binding.TempFeels.text = tempFeels
        binding.TempMin.text = tempMinCut
        binding.TempMax.text = tempMaxCut
        binding.Daytime.text = sunriseSunset
        val windText = getString(R.string.wind) + " (${gf.degToCompass(windDegree)})"
        binding.WindText.text = windText
        binding.Wind.text = windSpeed
        binding.Pressure.text = pressure
        binding.Humidity.text = humidity
        binding.UV.text = uvIndex
        binding.UV.setTextColor(uvColor)
    }

    override fun onStop() {
        //kill update Looper, that app don't update/come in foreground when cached
        handlerThread.quit()
        super.onStop()
    }
}