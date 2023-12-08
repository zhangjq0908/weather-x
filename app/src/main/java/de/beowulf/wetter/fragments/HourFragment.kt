package de.beowulf.wetter.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.adapter.MyListHourAdapter
import de.beowulf.wetter.databinding.WeatherForecastBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HourFragment : Fragment() {

    private lateinit var binding: WeatherForecastBinding

    private val hourTime = arrayOfNulls<String>(48)
    private val hourDay = arrayOfNulls<String>(48)
    private val statusHour = arrayOfNulls<Int>(48)
    private val statusHourTint = arrayOfNulls<Int>(48)
    private val statusHourText = arrayOfNulls<String>(48)
    private val temp = arrayOfNulls<String>(48)
    private val wind = arrayOfNulls<String>(48)
    private val rainSnow = arrayOfNulls<String>(48)
    private val rainSnowType = arrayOfNulls<String>(48)
    private val humidity = arrayOfNulls<String>(48)
    private val pressure = arrayOfNulls<String>(48)
    private val uvIndex = arrayOfNulls<String>(48)
    private val uvColor = arrayOfNulls<Int>(48)
    private val windGust = arrayOfNulls<String>(48)
    private val cloudiness = arrayOfNulls<String>(48)
    private val visibility = arrayOfNulls<String>(48)
    private val gf = GlobalFunctions()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WeatherForecastBinding.inflate(layoutInflater)
        val view: View = binding.root

        gf.initializeContext(requireContext())

        val jsonObj = gf.result()

        binding.ListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, v, _, _ ->
                val moreInfo = v.findViewById<LinearLayout>(R.id.hourMoreInfo)
                if (moreInfo.visibility == View.GONE) {
                    moreInfo.visibility = View.VISIBLE
                } else {
                    moreInfo.visibility = View.GONE
                }
            }

        val date: String =
            SimpleDateFormat(
                getString(R.string.daymonthdate),
                Locale.getDefault()
            ).format(Date())
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " "

        var setTomorrow = false

        for (i: Int in 0..47) {
            val hourly: JSONObject = jsonObj.getJSONArray("hourly").getJSONObject(i)
            val hourlyWeather: JSONObject = hourly.getJSONArray("weather").getJSONObject(0)

            val icon: Int = gf.icon(hourlyWeather.getString("icon"))

            /* Populating extracted data into our views */
            hourTime[i] = SimpleDateFormat(gf.getTime(), Locale.getDefault()).format(
                Date(
                    hourly.getLong("dt") * 1000
                )
            ) + " "
            hourDay[i] =
                SimpleDateFormat(getString(R.string.daymonthdate), Locale.getDefault()).format(
                    Date(
                        hourly.getLong("dt") * 1000
                    )
                )
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " "

            if (hourDay[i] == date) {
                hourDay[i] = getString(R.string.today)
                setTomorrow = true
            } else {
                if ((hourTime[i] == "00:00 " || hourTime[i] == "01:00 AM ") && setTomorrow) {
                    hourDay[i] = getString(R.string.tomorrow)
                    setTomorrow = false
                }
            }

            val windDouble: Double = hourly.getDouble("wind_speed")
            val windDegree: Int = hourly.getInt("wind_deg")
            statusHour[i] = icon
            statusHourTint[i] = gf.iconTint(hourlyWeather.getString("icon"))
            statusHourText[i] =
                hourlyWeather.getString("description")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            temp[i] = gf.convertTemp(hourly.getDouble("temp"))
            wind[i] = gf.convertSpeed(windDouble) + " (${gf.degToCompass(windDegree)})"
            if (hourly.has("wind_gust")) {
                windGust[i] = gf.convertSpeed(hourly.getDouble("wind_gust"))
            }
            humidity[i] = hourly.getString("humidity") + "%"
            pressure[i] = gf.convertPressure(hourly.getDouble("pressure"))
            cloudiness[i] = hourly.getString("clouds") + "%"
            uvIndex[i] = hourly.getString("uvi")
            uvColor[i] = when (hourly.getInt("uvi")) {
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
            val visibilityDouble: Double = hourly.getDouble("visibility") / 1000
            visibility[i] = String.format("%.2f", visibilityDouble)
            visibility[i] = if (visibility[i] == "10,00") {
                ">10"
            } else {
                visibility[i]
            } + "km"
            val precipitation: Double = hourly.getDouble("pop") * 100
            when {
                hourly.has("rain") -> {
                    rainSnow[i] = gf.convertRain(hourly.getJSONObject("rain").getDouble("1h"))
                    rainSnowType[i] = "rain"
                }
                hourly.has("snow") -> {
                    rainSnow[i] = gf.convertRain(hourly.getJSONObject("snow").getDouble("1h"))
                    rainSnowType[i] = "snow"
                }
                else -> {
                    rainSnow[i] = gf.convertRain(0.0)
                    rainSnowType[i] = "rain"
                }
            }
            rainSnow[i] = rainSnow[i] + " (${precipitation.toString().split(".")[0]}%)"
        }

        val myListAdapter = MyListHourAdapter(
            requireActivity(),
            hourTime,
            hourDay,
            statusHour,
            statusHourTint,
            statusHourText,
            temp,
            wind,
            rainSnow,
            rainSnowType,
            humidity,
            pressure,
            uvIndex,
            uvColor,
            windGust,
            cloudiness,
            visibility
        )
        binding.ListView.adapter = myListAdapter

        return view
    }
}