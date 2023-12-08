package de.beowulf.wetter.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.adapter.MyListAlertsAdapter
import de.beowulf.wetter.databinding.WeatherForecastBinding
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class WeatherAlertActivity : AppCompatActivity() {

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

        val alertsRaw = JSONArray(intent.getStringExtra("alert"))

        val alerts = arrayOfNulls<Warning>(alertsRaw.length())
        for (i in 0 until alertsRaw.length()) {
            val alert = alertsRaw.getJSONObject(i)
            var time = ""
            for (type in listOf("start", "end")) {
                time += SimpleDateFormat(
                    "${getString(R.string.date)} ${gf.getTime()}",
                    Locale.ROOT
                ).format(Date(alert.getLong(type) * 1000))
                if (type == "start")
                    time += " - "
            }
            alerts[i] = Warning(
                alert.getString("event"),
                " - " + alert.getString("sender_name"),
                time,
                alert.getString("description")
            )
        }
        alerts.sort()
        val currentDate = SimpleDateFormat(
            getString(R.string.date),
            Locale.getDefault()
        ).format(System.currentTimeMillis()) + " "
        val myListAdapter = MyListAlertsAdapter(
            this,
            alerts,
            currentDate
        )
        binding.ListView.adapter = myListAdapter
    }

    class Warning(val event: String, val source: String, val time: String, val description: String) : Comparable<Warning> {
        override fun compareTo(other: Warning): Int = this.time.compareTo(other.time)
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}