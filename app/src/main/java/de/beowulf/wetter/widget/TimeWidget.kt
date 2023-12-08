package de.beowulf.wetter.widget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.getSystemService
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.activities.LaunchActivity
import de.beowulf.wetter.R
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnspecifiedImmutableFlag")
class TimeWidget : WidgetProvider() {

    private val gf = GlobalFunctions()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        gf.initializeContext(context)
        val views = RemoteViews(context.packageName, R.layout.time_widget)

        resizeWidget(newOptions, views)

        //When API level 19 or higher, open Alarm Settings, when you click on the clock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val timeIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            val timePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(context, 0, timeIntent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(context, 0, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            views.setOnClickPendingIntent(R.id.Clock, timePendingIntent)
            views.setOnClickPendingIntent(R.id.AlarmIcon, timePendingIntent)
            views.setOnClickPendingIntent(R.id.nextAlarm, timePendingIntent)
        }
        //Open calendar, when you click on the time
        val dateIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        val datePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, 0, dateIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, dateIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        views.setOnClickPendingIntent(R.id.dayTextClock, datePendingIntent)
        //Start app, when you click on the widget
        val configIntent = Intent(context, LaunchActivity::class.java)
        val configPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        views.setOnClickPendingIntent(R.id.TimeWidget, configPendingIntent)

        // Instruct the widget provider to update the widget
        sendIntents(context, TimeWidget::class.java)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val gf = GlobalFunctions()
        gf.initializeContext(context)
        val views = RemoteViews(context.packageName, R.layout.time_widget)

        if (gf.getInitialized()) {
            val jsonObj = gf.result()

            //get data
            val current: JSONObject = jsonObj.getJSONObject("current")
            val currentWeather: JSONObject = current.getJSONArray("weather").getJSONObject(0)

            val nextAlarm: String? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getSystemService(
                        context,
                        AlarmManager::class.java
                    )?.nextAlarmClock?.triggerTime != null
                ) {
                    SimpleDateFormat("E, ${gf.getTime()}", Locale.getDefault()).format(
                        Date(
                            getSystemService(
                                context,
                                AlarmManager::class.java
                            )!!.nextAlarmClock.triggerTime
                        )
                    )
                } else {
                    null
                }

            val updatedAt: Long = current.getLong("dt")
            val sdf = SimpleDateFormat(context.getString(R.string.date), Locale.getDefault())
            val updatedAtText: String = context.getString(R.string.updated) + " " +
                    if (sdf.format(System.currentTimeMillis()).equals(sdf.format(updatedAt * 1000))) {
                        SimpleDateFormat(
                            gf.getTime(),
                            Locale.ROOT
                        ).format(Date(updatedAt * 1000))
                    } else {
                        SimpleDateFormat(
                            "${context.getString(R.string.date).dropLast(4)} ${gf.getTime()}",
                            Locale.ROOT
                        ).format(Date(updatedAt * 1000))
                    }

            val image: Int = gf.icon(currentWeather.getString("icon"))
            val actualTemp: String = gf.convertTemp(current.getDouble("temp"))
            val statusText: String = currentWeather.getString("description").replace(" ", "\n")
            val wind: String =
                gf.convertSpeed(current.getDouble("wind_speed")) + " (${
                    gf.degToCompass(current.getInt("wind_deg"))
                })"

            var rainSnow: String
            var type = "snow"
            when {
                current.has("rain") -> {
                    rainSnow = gf.convertRain(current.getJSONObject("rain").getDouble("1h"))
                    type = "rain"
                }
                current.has("snow") -> {
                    rainSnow = gf.convertRain(current.getJSONObject("snow").getDouble("1h"))
                }
                else -> {
                    rainSnow = gf.convertRain(0.0)
                    type = "rain"
                }
            }
            if (type == "rain") {
                views.setViewVisibility(R.id.Rain, View.VISIBLE)
                views.setViewVisibility(R.id.Snow, View.GONE)
            } else {
                views.setViewVisibility(R.id.Rain, View.GONE)
                views.setViewVisibility(R.id.Snow, View.VISIBLE)
            }
            val precipitation: Double =
                jsonObj.getJSONArray("hourly").getJSONObject(0).getDouble("pop") * 100
            rainSnow += " (${precipitation.toString().split(".")[0]}%)"

            for (appWidgetId in appWidgetIds) {
                val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
                resizeWidget(appWidgetOptions, views)

                val widgetSettings = gf.widgetSettings(appWidgetId)
                var highlightDay = false
                var color = Color.WHITE
                if (widgetSettings != null) {
                    if (widgetSettings.length > WidgetConf.HIDE_UPDATED.ordinal &&
                        widgetSettings[WidgetConf.HIDE_UPDATED.ordinal] == '1') {
                        views.setViewVisibility(R.id.update, View.GONE)
                    }
                    if (widgetSettings.length > WidgetConf.HIGHLIGHT_DAY.ordinal &&
                        widgetSettings[WidgetConf.HIGHLIGHT_DAY.ordinal] == '1') {
                        highlightDay = true
                    }
                    if (widgetSettings.split(';').size > 1) {
                        color = widgetSettings.split(';')[1].toInt()
                    }
                }

                //set View
                if (nextAlarm == null) {
                    views.setViewVisibility(R.id.AlarmIcon, View.GONE)
                    views.setViewVisibility(R.id.nextAlarm, View.GONE)
                } else {
                    views.setViewVisibility(R.id.AlarmIcon, View.VISIBLE)
                    views.setViewVisibility(R.id.nextAlarm, View.VISIBLE)
                    views.setTextViewText(R.id.nextAlarm, nextAlarm)
                }
                views.setImageViewResource(R.id.Status_Image, image)
                views.setTextViewText(R.id.actualTemp, actualTemp)
                views.setTextViewText(R.id.Status_Text, statusText)
                views.setTextViewText(R.id.Wind, wind)
                views.setTextViewText(R.id.RainSnow, rainSnow)
                views.setTextViewText(R.id.UpdatedAt, updatedAtText)
                //set front color
                views.setTextColor(R.id.Clock, color)
                if (highlightDay) {
                    views.setTextColor(R.id.dayTextClock, Color.YELLOW)
                    views.setInt(R.id.AlarmIcon, "setColorFilter", Color.YELLOW)
                    views.setTextColor(R.id.nextAlarm, Color.YELLOW)
                } else {
                    views.setTextColor(R.id.dayTextClock, color)
                    views.setInt(R.id.AlarmIcon, "setColorFilter", color)
                    views.setTextColor(R.id.nextAlarm, color)
                }
                views.setInt(R.id.Status_Image, "setColorFilter", color)
                views.setTextColor(R.id.actualTemp, color)
                views.setTextColor(R.id.Status_Text, color)
                views.setInt(R.id.Snow, "setColorFilter", color)
                views.setInt(R.id.Rain, "setColorFilter", color)
                views.setTextColor(R.id.RainSnow, color)
                views.setInt(R.id.WindIcon, "setColorFilter", color)
                views.setTextColor(R.id.Wind, color)
                views.setTextColor(R.id.UpdatedAt, color)
                views.setInt(R.id.UpdatedIcon, "setColorFilter", color)

                //When API level 19 or higher, open Alarm Settings, when you click on the clock
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val timeIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    val timePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.getActivity(context, 0, timeIntent, PendingIntent.FLAG_IMMUTABLE)
                    } else {
                        PendingIntent.getActivity(context, 0, timeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    }
                    views.setOnClickPendingIntent(R.id.Clock, timePendingIntent)
                    views.setOnClickPendingIntent(R.id.AlarmIcon, timePendingIntent)
                    views.setOnClickPendingIntent(R.id.nextAlarm, timePendingIntent)
                }
                //Open calendar, when you click on the time
                val dateIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
                val datePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getActivity(context, 0, dateIntent, PendingIntent.FLAG_IMMUTABLE)
                } else {
                    PendingIntent.getActivity(context, 0, dateIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                views.setOnClickPendingIntent(R.id.dayTextClock, datePendingIntent)
                //Start app, when you click on the widget
                val configIntent = Intent(context, LaunchActivity::class.java)
                val configPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE)
                } else {
                    PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                views.setOnClickPendingIntent(R.id.TimeWidget, configPendingIntent)
                //Update widget, when user click on update button
                val conInt = Intent(context, WidgetProvider::class.java)
                conInt.action = ACTION_AUTO_UPDATE
                val conPenInt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getBroadcast(context, 0, conInt, PendingIntent.FLAG_IMMUTABLE)
                } else {
                    PendingIntent.getBroadcast(context, 0, conInt, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                views.setOnClickPendingIntent(R.id.update, conPenInt)

                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

private fun resizeWidget(appWidgetOptions: Bundle, views: RemoteViews) {
    val minWidth: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    /*val maxWidth: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
    val minHeight: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)*/
    val maxHeight: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

    when {
        minWidth < 270 || maxHeight < 180 -> {
            views.setViewVisibility(R.id.WindRain, View.GONE)
        }
        else -> {
            views.setViewVisibility(R.id.WindRain, View.VISIBLE)
        }
    }
    when {
        maxHeight < 115 -> {
            views.setTextViewTextSize(R.id.Clock, TypedValue.COMPLEX_UNIT_SP, 45F)
            views.setViewVisibility(R.id.update, View.GONE)
        }
        maxHeight > 250 && minWidth > 250 -> {
            views.setTextViewTextSize(R.id.Clock, TypedValue.COMPLEX_UNIT_SP, 100F)
            views.setTextViewTextSize(R.id.dayTextClock, TypedValue.COMPLEX_UNIT_SP, 15F)
            views.setTextViewTextSize(R.id.nextAlarm, TypedValue.COMPLEX_UNIT_SP, 15F)
            views.setViewVisibility(R.id.Space, View.VISIBLE)
            views.setViewVisibility(R.id.update, View.VISIBLE)
        }
        maxHeight > 115 -> {
            views.setTextViewTextSize(R.id.Clock, TypedValue.COMPLEX_UNIT_SP, 70F)
            views.setTextViewTextSize(R.id.dayTextClock, TypedValue.COMPLEX_UNIT_SP, 12F)
            views.setTextViewTextSize(R.id.nextAlarm, TypedValue.COMPLEX_UNIT_SP, 12F)
            views.setViewVisibility(R.id.Space, View.GONE)
            views.setViewVisibility(R.id.update, View.VISIBLE)
        }
    }
}
