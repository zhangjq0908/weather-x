package de.beowulf.wetter.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.beowulf.wetter.R
import de.beowulf.wetter.activities.WeatherAlertActivity.*

class MyListAlertsAdapter(
    private val context: Activity,
    private val alerts: Array<Warning?>,
    private val currentDate: String
) : ArrayAdapter<Warning>(context, R.layout.listview_warnings, alerts) {

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = context.layoutInflater
        val rowView: View = inflater.inflate(R.layout.listview_warnings, null, true)

        val eventTV: TextView = rowView.findViewById(R.id.event) as TextView
        val sourceTV: TextView = rowView.findViewById(R.id.source) as TextView
        val timeTV: TextView = rowView.findViewById(R.id.time) as TextView
        val descriptionTV: TextView = rowView.findViewById(R.id.description) as TextView

        eventTV.text = alerts[position]?.event ?: "error"
        sourceTV.text = alerts[position]?.source ?: "error"
        timeTV.text = alerts[position]?.time?.replace(currentDate, "") ?: "error"
        descriptionTV.text = alerts[position]?.description ?: "error"

        return rowView
    }
}