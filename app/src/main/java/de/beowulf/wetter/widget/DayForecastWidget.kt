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
class DayForecastWidget : WidgetProvider() {

    private val gf = GlobalFunctions()

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        gf.initializeContext(context)
        val views = RemoteViews(context.packageName, R.layout.forecast_widget)

        resizeWidget(newOptions, views)
        //Start app, when you click on the widget
        val configIntent = Intent(context, LaunchActivity::class.java)
        val configPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        views.setOnClickPendingIntent(R.id.forecast_widget, configPendingIntent)

        // Instruct the widget provider to update the widget
        sendIntents(context, DayForecastWidget::class.java)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val gf = GlobalFunctions()
        val views = RemoteViews(context.packageName, R.layout.forecast_widget)

        gf.initializeContext(context)

        val day = arrayOfNulls<String>(8)
        val image = arrayOfNulls<Int>(8)
        val maxTemp = arrayOfNulls<String>(8)
        val minTemp = arrayOfNulls<String>(8)

        val jsonObj = gf.result()

        val updatedAt: Long = jsonObj.getJSONObject("current").getLong("dt")
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

        for (i: Int in 1..7) {
            val daily: JSONObject = jsonObj.getJSONArray("daily").getJSONObject(i)
            val dailyTemp: JSONObject = daily.getJSONObject("temp")
            val dailyWeather: JSONObject = daily.getJSONArray("weather").getJSONObject(0)

            day[i] = SimpleDateFormat(context.getString(R.string.daydate), Locale.getDefault()).format(
                    Date(
                        daily.getLong("dt") * 1000
                    )
                )

            image[i] = gf.icon(dailyWeather.getString("icon"))

            maxTemp[i] = gf.convertTemp(dailyTemp.getDouble("max"))
            minTemp[i] = gf.convertTemp(dailyTemp.getDouble("min"))
        }

        for (appWidgetId in appWidgetIds) {
            val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
            resizeWidget(appWidgetOptions, views)

            val widgetSettings = gf.widgetSettings(appWidgetId)
            var color = Color.WHITE
            if (widgetSettings != null) {
                if (widgetSettings.length > WidgetConf.BACKGROUND.ordinal &&
                    widgetSettings[WidgetConf.BACKGROUND.ordinal] == '1') {
                    views.setInt(R.id.forecast_widget, "setBackgroundResource", Color.TRANSPARENT)
                }
                if (widgetSettings.length > WidgetConf.HIDE_UPDATED.ordinal &&
                    widgetSettings[WidgetConf.HIDE_UPDATED.ordinal] == '1') {
                    views.setViewVisibility(R.id.update, View.GONE)
                }
                if (widgetSettings.split(';').size > 1) {
                    color = widgetSettings.split(';')[1].toInt()
                }
            }

            views.setTextViewText(R.id.UpdatedAt, updatedAtText)
            //set View
            // next item:
            views.setTextViewText(R.id.day1, day[1])
            image[1]?.let {views.setImageViewResource(R.id.dayImage1, it)}
            views.setTextViewText(R.id.dayMax1, maxTemp[1])
            views.setTextViewText(R.id.dayMin1, minTemp[1])
            //color
            views.setTextColor(R.id.day1, color)
            views.setInt(R.id.dayImage1, "setColorFilter", color)
            views.setTextColor(R.id.dayMax1, color)
            views.setTextColor(R.id.dayMin1, color)
            // next item:
            views.setTextViewText(R.id.day2, day[2])
            image[2]?.let {views.setImageViewResource(R.id.dayImage2, it)}
            views.setTextViewText(R.id.dayMax2, maxTemp[2])
            views.setTextViewText(R.id.dayMin2, minTemp[2])
            //color
            views.setTextColor(R.id.day2, color)
            views.setInt(R.id.dayImage2, "setColorFilter", color)
            views.setTextColor(R.id.dayMax2, color)
            views.setTextColor(R.id.dayMin2, color)
            // next item:
            views.setTextViewText(R.id.day3, day[3])
            image[3]?.let {views.setImageViewResource(R.id.dayImage3, it)}
            views.setTextViewText(R.id.dayMax3, maxTemp[3])
            views.setTextViewText(R.id.dayMin3, minTemp[3])
            //color
            views.setTextColor(R.id.day3, color)
            views.setInt(R.id.dayImage3, "setColorFilter", color)
            views.setTextColor(R.id.dayMax3, color)
            views.setTextColor(R.id.dayMin3, color)
            // next item:
            views.setTextViewText(R.id.day4, day[4])
            image[4]?.let {views.setImageViewResource(R.id.dayImage4, it)}
            views.setTextViewText(R.id.dayMax4, maxTemp[4])
            views.setTextViewText(R.id.dayMin4, minTemp[4])
            //color
            views.setTextColor(R.id.day4, color)
            views.setInt(R.id.dayImage4, "setColorFilter", color)
            views.setTextColor(R.id.dayMax4, color)
            views.setTextColor(R.id.dayMin4, color)
            // next item:
            views.setTextViewText(R.id.day5, day[5])
            image[5]?.let {views.setImageViewResource(R.id.dayImage5, it)}
            views.setTextViewText(R.id.dayMax5, maxTemp[5])
            views.setTextViewText(R.id.dayMin5, minTemp[5])
            //color
            views.setTextColor(R.id.day5, color)
            views.setInt(R.id.dayImage5, "setColorFilter", color)
            views.setTextColor(R.id.dayMax5, color)
            views.setTextColor(R.id.dayMin5, color)
            // next item:
            views.setTextViewText(R.id.day6, day[6])
            image[6]?.let {views.setImageViewResource(R.id.dayImage6, it)}
            views.setTextViewText(R.id.dayMax6, maxTemp[6])
            views.setTextViewText(R.id.dayMin6, minTemp[6])
            //color
            views.setTextColor(R.id.day6, color)
            views.setInt(R.id.dayImage6, "setColorFilter", color)
            views.setTextColor(R.id.dayMax6, color)
            views.setTextColor(R.id.dayMin6, color)
            //updated color
            views.setTextColor(R.id.UpdatedAt, color)
            views.setInt(R.id.UpdatedIcon, "setColorFilter", color)
            // next item:
            views.setTextViewText(R.id.day7, day[7])
            image[7]?.let {views.setImageViewResource(R.id.dayImage7, it)}
            views.setTextViewText(R.id.dayMax7, maxTemp[7])
            views.setTextViewText(R.id.dayMin7, minTemp[7])
            //color
            views.setTextColor(R.id.day7, color)
            views.setInt(R.id.dayImage7, "setColorFilter", color)
            views.setTextColor(R.id.dayMax7, color)
            views.setTextColor(R.id.dayMin7, color)

            //Start app, when you click on the widget
            val configIntent = Intent(context, LaunchActivity::class.java)
            val configPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            views.setOnClickPendingIntent(R.id.forecast_widget, configPendingIntent)
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

private fun resizeWidget(appWidgetOptions: Bundle, views: RemoteViews) {
    val minWidth: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    /*val maxWidth: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
    val minHeight: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)*/
    val maxHeight: Int = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    //first make all gone
    views.setViewVisibility(R.id.item2, View.GONE)
    views.setViewVisibility(R.id.item3, View.GONE)
    views.setViewVisibility(R.id.item4, View.GONE)
    views.setViewVisibility(R.id.item5, View.GONE)
    views.setViewVisibility(R.id.item6, View.GONE)
    views.setViewVisibility(R.id.item7, View.GONE)

    when {
        minWidth < 130 -> {
        }
        minWidth < 195 -> {
            views.setViewVisibility(R.id.item2, View.VISIBLE)
        }
        minWidth < 260 -> {
            views.setViewVisibility(R.id.item2, View.VISIBLE)
            views.setViewVisibility(R.id.item3, View.VISIBLE)
        }
        minWidth < 325 -> {
            views.setViewVisibility(R.id.item2, View.VISIBLE)
            views.setViewVisibility(R.id.item3, View.VISIBLE)
            views.setViewVisibility(R.id.item4, View.VISIBLE)
        }
        minWidth < 390 -> {
            views.setViewVisibility(R.id.item2, View.VISIBLE)
            views.setViewVisibility(R.id.item3, View.VISIBLE)
            views.setViewVisibility(R.id.item4, View.VISIBLE)
            views.setViewVisibility(R.id.item5, View.VISIBLE)
        }
        minWidth < 455 -> {
            views.setViewVisibility(R.id.item2, View.VISIBLE)
            views.setViewVisibility(R.id.item3, View.VISIBLE)
            views.setViewVisibility(R.id.item4, View.VISIBLE)
            views.setViewVisibility(R.id.item5, View.VISIBLE)
            views.setViewVisibility(R.id.item6, View.VISIBLE)
        }
        minWidth > 455 -> {
            views.setViewVisibility(R.id.item2, View.VISIBLE)
            views.setViewVisibility(R.id.item3, View.VISIBLE)
            views.setViewVisibility(R.id.item4, View.VISIBLE)
            views.setViewVisibility(R.id.item5, View.VISIBLE)
            views.setViewVisibility(R.id.item6, View.VISIBLE)
            views.setViewVisibility(R.id.item7, View.VISIBLE)
        }
    }

    //first make all gone
    views.setViewVisibility(R.id.dayMax1, View.GONE)
    views.setViewVisibility(R.id.dayMin1, View.GONE)
    views.setViewVisibility(R.id.dayMax2, View.GONE)
    views.setViewVisibility(R.id.dayMin2, View.GONE)
    views.setViewVisibility(R.id.dayMax3, View.GONE)
    views.setViewVisibility(R.id.dayMin3, View.GONE)
    views.setViewVisibility(R.id.dayMax4, View.GONE)
    views.setViewVisibility(R.id.dayMin4, View.GONE)
    views.setViewVisibility(R.id.dayMax5, View.GONE)
    views.setViewVisibility(R.id.dayMin5, View.GONE)
    views.setViewVisibility(R.id.dayMax6, View.GONE)
    views.setViewVisibility(R.id.dayMin6, View.GONE)
    views.setViewVisibility(R.id.dayMax7, View.GONE)
    views.setViewVisibility(R.id.dayMin7, View.GONE)

    when {
        maxHeight < 80 -> {
        }
        maxHeight < 110 -> {
            views.setViewVisibility(R.id.dayMax1, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax2, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax3, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax4, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax5, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax6, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax7, View.VISIBLE)
        }
        maxHeight > 110 -> {
            views.setViewVisibility(R.id.dayMax1, View.VISIBLE)
            views.setViewVisibility(R.id.dayMin1, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax2, View.VISIBLE)
            views.setViewVisibility(R.id.dayMin2, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax3, View.VISIBLE)
            views.setViewVisibility(R.id.dayMin3, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax4, View.VISIBLE)
            views.setViewVisibility(R.id.dayMin4, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax5, View.VISIBLE)
            views.setViewVisibility(R.id.dayMin5, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax6, View.VISIBLE)
            views.setViewVisibility(R.id.dayMin6, View.VISIBLE)
            views.setViewVisibility(R.id.dayMax7, View.VISIBLE)
            views.setViewVisibility(R.id.dayMin7, View.VISIBLE)
        }
    }
}
