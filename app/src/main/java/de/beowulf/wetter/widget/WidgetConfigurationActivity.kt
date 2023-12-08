package de.beowulf.wetter.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.databinding.ActivityWidgetConfigurationBinding

enum class WidgetConf {
    BACKGROUND,
    HIDE_UPDATED,
    HIGHLIGHT_DAY
}

private enum class Widgets {
    APP,
    TIME,
    DAY_FORECAST,
    HOUR_FORECAST
}

class WidgetConfigurationActivity : Activity() {

    private lateinit var binding: ActivityWidgetConfigurationBinding

    var red = 255
    var green = 255
    var blue = 255

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(GlobalFunctions().getTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.setFinishOnTouchOutside(true)

        setResult(RESULT_CANCELED)
        val sP = this.getSharedPreferences("de.beowulf.wetter", 0)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val widget: Widgets = when (AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId).previewImage) {
            R.drawable.timewidget_preview -> Widgets.TIME
            R.drawable.dayforecastwidget_preview -> Widgets.DAY_FORECAST
            R.drawable.hourforecastwidget_preview -> Widgets.HOUR_FORECAST
            else -> Widgets.APP
        }

        if (widget == Widgets.APP || widget == Widgets.TIME)
            binding.backTrans.visibility = View.GONE

        if (widget == Widgets.TIME)
            binding.highlightDay.visibility = View.VISIBLE

        var intervalMinutes: Long = sP.getInt("widget-update", if(sP.getString("api", "") == this.getString(R.string.standardKey)) {
            60
        } else {
            15
        }).toLong()
        binding.widgetUpdate.setText(intervalMinutes.toString())

        val widgetSettings = sP.getString("widget_$appWidgetId", "000")
        if (widgetSettings != null) {
            binding.backTrans.isChecked = widgetSettings[WidgetConf.BACKGROUND.ordinal] == '1'
            binding.hideUpdate.isChecked = widgetSettings[WidgetConf.HIDE_UPDATED.ordinal] == '1'
            binding.highlightDay.isChecked = widgetSettings[WidgetConf.HIGHLIGHT_DAY.ordinal] == '1'
        }

        binding.red.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun afterTextChanged(edit: Editable) {
                red = if (edit.toString().isNotEmpty()) edit.toString().toInt() else 0
                generatePreview()
            }
        })

        binding.green.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun afterTextChanged(edit: Editable) {
                green = if (edit.toString().isNotEmpty()) edit.toString().toInt() else 0
                generatePreview()
            }
        })

        binding.blue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun afterTextChanged(edit: Editable) {
                blue = if (edit.toString().isNotEmpty()) edit.toString().toInt() else 0
                generatePreview()
            }
        })

        binding.Save.setOnClickListener {
            val backTrans = if (binding.backTrans.isChecked) "1" else "0"
            val hideUpdate = if (binding.hideUpdate.isChecked) "1" else "0"
            val highlightDay = if (binding.highlightDay.isChecked) "1" else "0"

            val updateInterval: String = binding.widgetUpdate.text.toString()
            if (updateInterval.isNotBlank() && updateInterval.toIntOrNull() != null) {
                intervalMinutes = updateInterval.toLong()

                if (sP.getString("api", "") == this.getString(R.string.standardKey) &&
                    intervalMinutes < 30) {
                    intervalMinutes = 30
                    Toast.makeText(this, this.getString(R.string.widgetUpdateIntervalToShort), Toast.LENGTH_LONG).show()
                }
                if (intervalMinutes < 1) //set to min 1min
                    intervalMinutes = 1
            } else if (sP.getString("api", "") == this.getString(R.string.standardKey) &&
                intervalMinutes < 30) {
                intervalMinutes = 30
            }

            val appWidgetAlarm = WidgetUpdater(applicationContext)
            appWidgetAlarm.startAlarm(intervalMinutes)

            sP.edit()
                .putString(
                    "widget_$appWidgetId",
                    "$backTrans$hideUpdate$highlightDay;${Color.rgb(red, green, blue)}"
                )
                .putInt("widget-update", intervalMinutes.toInt())
                .apply()

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            WidgetProvider().updateWidgets(applicationContext)
            finish()
        }
    }

    private fun generatePreview() {
        if (red.noValidColor()) {
            binding.red.setText("255")
        } else if (green.noValidColor()) {
            binding.green.setText("255")
        } else if (blue.noValidColor()) {
            binding.blue.setText("255")
        } else {
            val drawable = ColorDrawable()
            drawable.color = Color.rgb(red, green, blue)
            binding.colorPreview.background = drawable
        }
    }
}

fun Int.noValidColor(): Boolean {
    return this !in 0..255
}
