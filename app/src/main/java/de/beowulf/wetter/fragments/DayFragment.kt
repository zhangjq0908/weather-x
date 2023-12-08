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
import de.beowulf.wetter.adapter.MyListDayCitiesAdapter
import de.beowulf.wetter.databinding.WeatherForecastBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class DayFragment : Fragment() {

    private lateinit var binding: WeatherForecastBinding

    private val dayDate = arrayOfNulls<String>(8)
    private val weekend = arrayOfNulls<Boolean>(8)
    private val statusDay = arrayOfNulls<Int>(8)
    private val statusDayTint = arrayOfNulls<Int>(8)
    private val statusDayText = arrayOfNulls<String>(8)
    private val minTemp = arrayOfNulls<String>(8)
    private val maxTemp = arrayOfNulls<String>(8)
    private val sunrise = arrayOfNulls<String>(8)
    private val sunset = arrayOfNulls<String>(8)
    private val wind = arrayOfNulls<String>(8)
    private val humidity = arrayOfNulls<String>(8)
    private val pressure = arrayOfNulls<String>(8)
    private val uvIndex = arrayOfNulls<String>(8)
    private val uvColor = arrayOfNulls<Int>(8)
    private val windGust = arrayOfNulls<String>(8)
    private val rainSnow = arrayOfNulls<String>(8)
    private val rainSnowType = arrayOfNulls<String>(8)
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
                val moreInfo = v.findViewById<LinearLayout>(R.id.dayMoreInfo)
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

        for (i: Int in 0..7) {
            val daily: JSONObject = jsonObj.getJSONArray("daily").getJSONObject(i)
            val dailyTemp: JSONObject = daily.getJSONObject("temp")
            val dailyWeather: JSONObject = daily.getJSONArray("weather").getJSONObject(0)

            /* Populating extracted data into our views */
            if (dayDate[i] == null) // check if dayDate is null, maybe it's already "Tomorrow" (see below)
                dayDate[i] =
                    SimpleDateFormat(getString(R.string.daymonthdate), Locale.getDefault()).format(
                        Date(
                            daily.getLong("dt") * 1000
                        )
                    )
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " "
            val day: Calendar = Calendar.getInstance(Locale.getDefault())
            day.timeInMillis = daily.getLong("dt") * 1000
            weekend[i] = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

            if (dayDate[i] == date) {
                dayDate[i] = getString(R.string.today) + " "
                if (i != 7) // set next item in array to Tomorrow, when current is Today (null checker above needed!)
                    dayDate[i + 1] = getString(R.string.tomorrow) + " "
            }

            val windDegree: Int = daily.getInt("wind_deg")
            val windDouble: Double = daily.getDouble("wind_speed")
            val precipitation: Double = daily.getDouble("pop") * 100
            statusDay[i] = gf.icon(dailyWeather.getString("icon"))
            statusDayTint[i] = gf.iconTint(dailyWeather.getString("icon"))
            statusDayText[i] = dailyWeather.getString("description")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            minTemp[i] = gf.convertTemp(dailyTemp.getDouble("min"))
            maxTemp[i] = gf.convertTemp(dailyTemp.getDouble("max"))
            sunrise[i] = SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
                Date(
                    daily.getLong("sunrise") * 1000
                )
            )
            sunset[i] = SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
                Date(
                    daily.getLong("sunset") * 1000
                )
            )
            wind[i] = gf.convertSpeed(windDouble) + " (${gf.degToCompass(windDegree)})"
            if (daily.has("wind_gust")) {
                windGust[i] = gf.convertSpeed(daily.getDouble("wind_gust"))
            }
            pressure[i] = gf.convertPressure(daily.getDouble("pressure"))
            humidity[i] = daily.getString("humidity") + "%"
            uvIndex[i] = daily.getString("uvi")
            uvColor[i] = when (daily.getInt("uvi")) {
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
            when {
                daily.has("rain") -> {
                    rainSnow[i] = gf.convertRain(daily.getDouble("rain"))
                    rainSnowType[i] = "rain"
                }
                daily.has("snow") -> {
                    rainSnow[i] = gf.convertRain(daily.getDouble("snow"))
                    rainSnowType[i] = "snow"
                }
                else -> {
                    rainSnow[i] = gf.convertRain(0.0)
                    rainSnowType[i] = "rain"
                }
            }
            rainSnow[i] = rainSnow[i] + " (${precipitation.toString().split(".")[0]}%)"
        }

        val myListAdapter = MyListDayCitiesAdapter(
            requireActivity(),
            dayDate,
            weekend,
            statusDay,
            statusDayTint,
            statusDayText,
            minTemp,
            maxTemp,
            sunrise,
            sunset,
            wind,
            pressure,
            humidity,
            rainSnow,
            rainSnowType,
            uvIndex,
            uvColor,
            windGust
        )
        binding.ListView.adapter = myListAdapter

        return view
    }
}