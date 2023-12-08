package de.beowulf.wetter.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.beowulf.wetter.R

class MyListHourAdapter(
    private val context: Activity,
    private val hourTime: Array<String?>,
    private val hourDate: Array<String?>,
    private val statusHour: Array<Int?>,
    private val statusHourTint: Array<Int?>,
    private val statusHourText: Array<String?>,
    private val temp: Array<String?>,
    private val wind: Array<String?>,
    private val rainSnow: Array<String?>,
    private val rainSnowType: Array<String?>,
    private val humidity: Array<String?>,
    private val pressure: Array<String?>,
    private val uvIndex: Array<String?>,
    private val uvColor: Array<Int?>,
    private val windGust: Array<String?>,
    private val cloudiness: Array<String?>,
    private val visibility: Array<String?>,
) : ArrayAdapter<String>(context, R.layout.listview_hour, hourTime) {

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = context.layoutInflater
        val rowView: View = inflater.inflate(R.layout.listview_hour, null, true)

        val dateDividerLL: LinearLayout = rowView.findViewById(R.id.newDayDivider) as LinearLayout
        val hourDateTV: TextView = rowView.findViewById(R.id.HourDay) as TextView
        val hourTimeTV: TextView = rowView.findViewById(R.id.HourTime) as TextView
        val statusHourTV: ImageView = rowView.findViewById(R.id.StatusHour) as ImageView
        val statusHourTextTV: TextView = rowView.findViewById(R.id.StatusHourText) as TextView
        val actualTempTV: TextView = rowView.findViewById(R.id.actualTemp) as TextView
        val windTV: TextView = rowView.findViewById(R.id.Wind) as TextView
        val rainSnowTV: TextView = rowView.findViewById(R.id.RainSnow) as TextView
        val rainTV: ImageView = rowView.findViewById(R.id.Rain) as ImageView
        val snowTV: ImageView = rowView.findViewById(R.id.Snow) as ImageView
        val humidityTV: TextView = rowView.findViewById(R.id.Humidity) as TextView
        val pressureTV: TextView = rowView.findViewById(R.id.Pressure) as TextView
        val uvIndexTV: TextView = rowView.findViewById(R.id.UvIndex) as TextView
        val windGustGroup: LinearLayout = rowView.findViewById(R.id.WindGustGroup) as LinearLayout
        val windGustTV: TextView = rowView.findViewById(R.id.WindGust) as TextView
        val cloudinessTV: TextView = rowView.findViewById(R.id.Cloudiness) as TextView
        val visibilityTV: TextView = rowView.findViewById(R.id.Visibility) as TextView

        hourTimeTV.text = hourTime[position]
        if (position == 0 || hourTime[position] == "00:00 " || hourTime[position] == "12:00 AM ") {
            dateDividerLL.visibility = View.VISIBLE
            hourDateTV.text = hourDate[position]
            if (position == 0)
                dateDividerLL.setPadding(0,0,0,0)
        } else {
            dateDividerLL.visibility = View.GONE
        }
        statusHour[position]?.let { statusHourTV.setImageResource(it) }
        statusHourTint[position]?.let { statusHourTV.setColorFilter(it) }
        statusHourTextTV.text = statusHourText[position]
        actualTempTV.text = temp[position]
        windTV.text = wind[position]
        rainSnowTV.text = rainSnow[position]
        if (rainSnowType[position] == "rain") {
            rainTV.visibility = View.VISIBLE
            snowTV.visibility = View.GONE
        } else {
            rainTV.visibility = View.GONE
            snowTV.visibility = View.VISIBLE
        }
        humidityTV.text = humidity[position]
        pressureTV.text = pressure[position]
        uvIndexTV.text = uvIndex[position]
        uvIndexTV.setTextColor(uvColor[position]!!)
        if (windGust[position] != null) {
            windGustGroup.visibility = View.VISIBLE
            windGustTV.text = windGust[position]
        } else {
            windGustGroup.visibility = View.GONE
        }
        cloudinessTV.text = cloudiness[position]
        visibilityTV.text = visibility[position]

        return rowView
    }
}