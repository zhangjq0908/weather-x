package de.beowulf.wetter.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.activities.LaunchActivity
import de.beowulf.wetter.R
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnspecifiedImmutableFlag")
class AppWidget : WidgetProvider() {

    private val gf = GlobalFunctions()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        gf.initializeContext(context)
        val views = RemoteViews(context.packageName, R.layout.app_widget)

        resizeWidget(newOptions, views, gf.getTime())
        //Start app, when you click on the widget
        val configIntent = Intent(context, LaunchActivity::class.java)
        val configPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        views.setOnClickPendingIntent(R.id.AppWidget, configPendingIntent)

        // Instruct the widget provider to update the widget
        sendIntents(context, AppWidget::class.java)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val gf = GlobalFunctions()
        gf.initializeContext(context)
        val views = RemoteViews(context.packageName, R.layout.app_widget)

        if (gf.getInitialized()) {
            val jsonObj = gf.result()

            //get data
            val current: JSONObject = jsonObj.getJSONObject("current")
            val currentWeather: JSONObject = current.getJSONArray("weather").getJSONObject(0)
            val daily: JSONObject =
                jsonObj.getJSONArray("daily").getJSONObject(0).getJSONObject("temp")

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
            val statusText: String = currentWeather.getString("description")
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
            val sunrise: String =
                SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
                    Date(
                        current.getLong("sunrise") * 1000
                    )
                )
            val sunset: String =
                SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
                    Date(
                        current.getLong("sunset") * 1000
                    )
                )
            val pressure: String = gf.convertPressure(current.getDouble("pressure"))
            val humidity: String = current.getString("humidity") + "%"
            val maxMinTemp = "H:${gf.convertTemp(daily.getDouble("max"))} L:${gf.convertTemp(daily.getDouble("min"))}"

            for (appWidgetId in appWidgetIds) {
                val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
                resizeWidget(appWidgetOptions, views, gf.getTime())

                val widgetSettings = gf.widgetSettings(appWidgetId)
                var color = Color.WHITE
                if (widgetSettings != null) {
                    if (widgetSettings.length > WidgetConf.HIDE_UPDATED.ordinal &&
                        widgetSettings[WidgetConf.HIDE_UPDATED.ordinal] == '1') {
                        views.setViewVisibility(R.id.update, View.GONE)
                    }
                    if (widgetSettings.split(';').size > 1) {
                        color = widgetSettings.split(';')[1].toInt()
                    }
                }

                //set View
                views.setImageViewResource(R.id.Status_Image, image)
                views.setTextViewText(R.id.actualTemp, actualTemp)
                views.setTextViewText(R.id.Status_Text, statusText)
                views.setTextViewText(R.id.Wind, wind)
                views.setTextViewText(R.id.RainSnow, rainSnow)
                views.setTextViewText(R.id.Sunrise, sunrise)
                views.setTextViewText(R.id.Sunset, sunset)
                views.setTextViewText(R.id.Pressure, pressure)
                views.setTextViewText(R.id.Humidity, humidity)
                views.setTextViewText(R.id.maxMinTemp, maxMinTemp)
                views.setTextViewText(R.id.UpdatedAt, updatedAtText)
                //set front color
                views.setInt(R.id.Status_Image, "setColorFilter", color)
                views.setInt(R.id.WindIcon, "setColorFilter", color)
                views.setInt(R.id.Snow, "setColorFilter", color)
                views.setInt(R.id.Rain, "setColorFilter", color)
                views.setInt(R.id.SunriseIcon, "setColorFilter", color)
                views.setInt(R.id.SunsetIcon, "setColorFilter", color)
                views.setInt(R.id.PressureIcon, "setColorFilter", color)
                views.setInt(R.id.HumidityIcon, "setColorFilter", color)
                views.setInt(R.id.UpdatedIcon, "setColorFilter", color)
                views.setTextColor(R.id.actualTemp, color)
                views.setTextColor(R.id.Status_Text, color)
                views.setTextColor(R.id.Wind, color)
                views.setTextColor(R.id.RainSnow, color)
                views.setTextColor(R.id.Sunrise, color)
                views.setTextColor(R.id.Sunset, color)
                views.setTextColor(R.id.Pressure, color)
                views.setTextColor(R.id.Humidity, color)
                views.setTextColor(R.id.maxMinTemp, color)
                views.setTextColor(R.id.UpdatedAt, color)

                //Start app, when you click on the widget
                val configIntent = Intent(context, LaunchActivity::class.java)
                val configPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE)
                } else {
                    PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                views.setOnClickPendingIntent(R.id.AppWidget, configPendingIntent)
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

private fun resizeWidget(appWidgetOptions: Bundle, views: RemoteViews, time: String) {
    val minWidth: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    /*val maxWidth: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
    val minHeight: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)*/
    val maxHeight: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

    when {
        minWidth < 90 -> {
            views.setViewVisibility(R.id.actualTemp, View.GONE)
            views.setViewVisibility(R.id.Status_Text, View.GONE)
            views.setViewVisibility(R.id.WindRain, View.GONE)
            views.setViewVisibility(R.id.maxMinTemp, View.GONE)
            views.setViewVisibility(R.id.update, View.GONE)
        }
        minWidth < 175 -> {
            views.setViewVisibility(R.id.actualTemp, View.VISIBLE)
            views.setViewVisibility(R.id.Status_Text, View.VISIBLE)
            views.setViewVisibility(R.id.WindRain, View.GONE)
            views.setViewVisibility(R.id.maxMinTemp, View.GONE)
            views.setViewVisibility(R.id.update, View.VISIBLE)
        }
        minWidth < 245 -> {
            views.setViewVisibility(R.id.actualTemp, View.VISIBLE)
            views.setViewVisibility(R.id.Status_Text, View.VISIBLE)
            views.setViewVisibility(R.id.WindRain, View.GONE)
            views.setViewVisibility(R.id.maxMinTemp, View.VISIBLE)
            views.setViewVisibility(R.id.update, View.VISIBLE)
        }
        else -> {
            views.setViewVisibility(R.id.actualTemp, View.VISIBLE)
            views.setViewVisibility(R.id.Status_Text, View.VISIBLE)
            views.setViewVisibility(R.id.WindRain, View.VISIBLE)
            views.setViewVisibility(R.id.maxMinTemp, View.VISIBLE)
            views.setViewVisibility(R.id.update, View.VISIBLE)
        }
    }
    when {
        maxHeight < 110 || minWidth < 285 -> {
            views.setViewVisibility(R.id.moreInfos, View.GONE)
        }
        minWidth < 320 && time.contains('a') -> {
            views.setViewVisibility(R.id.moreInfos, View.GONE)
        }
        else -> {
            views.setViewVisibility(R.id.moreInfos, View.VISIBLE)
        }
    }
}
