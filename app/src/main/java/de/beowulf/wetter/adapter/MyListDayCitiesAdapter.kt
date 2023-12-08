package de.beowulf.wetter.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.beowulf.wetter.R

class MyListDayCitiesAdapter(
    private val context: Activity,
    private val dayDate: Array<String?>,
    private val weekend: Array<Boolean?>?,
    private val statusDay: Array<Int?>,
    private val statusDayTint: Array<Int?>,
    private val statusDayText: Array<String?>,
    private val min: Array<String?>,
    private val max: Array<String?>,
    private val sunrise: Array<String?>,
    private val sunset: Array<String?>,
    private val wind: Array<String?>,
    private val pressure: Array<String?>,
    private val humidity: Array<String?>,
    private val rainSnow: Array<String?>,
    private val rainSnowType: Array<String?>,
    private val uvIndex: Array<String?>?,
    private val uvColor: Array<Int?>?,
    private val windGust: Array<String?>
) : ArrayAdapter<String>(context, R.layout.listview_day_cities, dayDate) {

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = context.layoutInflater
        val rowView: View = inflater.inflate(R.layout.listview_day_cities, null, true)

        val dayDateTV: TextView = rowView.findViewById(R.id.DayDate) as TextView
        val statusDayTV: ImageView = rowView.findViewById(R.id.StatusDay) as ImageView
        val statusDayTextTV: TextView = rowView.findViewById(R.id.StatusDayText) as TextView
        val minTempTV: TextView = rowView.findViewById(R.id.minTemp) as TextView
        val maxTempTV: TextView = rowView.findViewById(R.id.maxTemp) as TextView
        val sunriseTV: TextView = rowView.findViewById(R.id.Sunrise) as TextView
        val sunsetTV: TextView = rowView.findViewById(R.id.Sunset) as TextView
        val windTV: TextView = rowView.findViewById(R.id.Wind) as TextView
        val pressureTV: TextView = rowView.findViewById(R.id.Pressure) as TextView
        val humidityTV: TextView = rowView.findViewById(R.id.Humidity) as TextView
        val rainSnowTV: TextView = rowView.findViewById(R.id.RainSnow) as TextView
        val rainTV: ImageView = rowView.findViewById(R.id.Rain) as ImageView
        val snowTV: ImageView = rowView.findViewById(R.id.Snow) as ImageView
        val uvIndexGroup: LinearLayout = rowView.findViewById(R.id.UvIndexGroup) as LinearLayout
        val uvIndexTV: TextView = rowView.findViewById(R.id.UvIndex) as TextView
        val windGustGroup: LinearLayout = rowView.findViewById(R.id.WindGustGroup) as LinearLayout
        val windGustTV: TextView = rowView.findViewById(R.id.WindGust) as TextView

        dayDateTV.text = dayDate[position]
        if (weekend?.get(position) == true) {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
            dayDateTV.setTextColor(typedValue.data)
        }
        statusDay[position]?.let { statusDayTV.setImageResource(it) }
        statusDayTint[position]?.let { statusDayTV.setColorFilter(it) }
        statusDayTextTV.text = statusDayText[position]
        minTempTV.text = min[position]
        maxTempTV.text = max[position]
        sunriseTV.text = sunrise[position]
        sunsetTV.text = sunset[position]
        windTV.text = wind[position]
        pressureTV.text = pressure[position]
        humidityTV.text = humidity[position]
        rainSnowTV.text = rainSnow[position]
        if (rainSnowType[position] == "rain") {
            rainTV.visibility = View.VISIBLE
            snowTV.visibility = View.GONE
        } else {
            rainTV.visibility = View.GONE
            snowTV.visibility = View.VISIBLE
        }
        if (uvIndex != null && uvColor != null) {
            uvIndexGroup.visibility = View.VISIBLE
            uvIndexTV.text = uvIndex[position]
            uvIndexTV.setTextColor(uvColor[position]!!)
        } else {
            uvIndexGroup.visibility = View.GONE
        }
        if (windGust[position] != null) {
            windGustGroup.visibility = View.VISIBLE
            windGustTV.text = windGust[position]
        } else {
            windGustGroup.visibility = View.GONE
        }

        return rowView
    }
}