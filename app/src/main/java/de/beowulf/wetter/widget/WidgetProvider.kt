package de.beowulf.wetter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

open class WidgetProvider : AppWidgetProvider() {

    private val gf = GlobalFunctions()

    override fun onReceive(context: Context, intent: Intent) {

        gf.initializeContext(context)

        if (gf.getInitialized()) {
            if (ACTION_AUTO_UPDATE == intent.action) {

                CoroutineScope(Dispatchers.IO).launch {
                    val result: String? = try {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        with(URL(gf.url("normal", "")).openConnection() as HttpsURLConnection) {
                            sslSocketFactory = gf.getSocketFactory()
                            requestMethod = "GET"
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        }
                    } catch (e: Exception) {
                        null
                    }
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            gf.setResult(result)
                        }
                        updateWidgets(context)
                    }
                }
            } else if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED" == intent.action ||
                    "android.appwidget.action.APPWIDGET_ENABLED" == intent.action){
                updateWidgets(context)
                super.onReceive(context, intent)
            } else {
                super.onReceive(context, intent)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val sP = context.getSharedPreferences("de.beowulf.wetter", 0)
        for (id in appWidgetIds)
            sP.edit().remove("widget_$id").apply()
        super.onDeleted(context, appWidgetIds)
    }

    fun updateWidgets(context: Context) {
        sendIntents(context, AppWidget::class.java)
        sendIntents(context, TimeWidget::class.java)
        sendIntents(context, HourForecastWidget::class.java)
        sendIntents(context, DayForecastWidget::class.java)
    }

    fun sendIntents(context: Context, widgetClass: Class<*>) {
        val intent = Intent(context.applicationContext, widgetClass)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        val ids = AppWidgetManager.getInstance(context.applicationContext)
            .getAppWidgetIds(ComponentName(context.applicationContext, widgetClass))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.applicationContext.sendBroadcast(intent)
    }

    override fun onDisabled(context: Context) {

        val appWidgetManager = AppWidgetManager.getInstance(context)

        val allWidgetIds =
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    AppWidget::class.java
                )
            ).size +
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    TimeWidget::class.java
                )
            ).size +
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    DayForecastWidget::class.java
                )
            ).size +
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    HourForecastWidget::class.java
                )
            ).size
        if (allWidgetIds == 0) {
            // stop alarm
            val appWidgetAlarm = WidgetUpdater(context.applicationContext)
            appWidgetAlarm.stopAlarm()

        }
    }

    override fun onEnabled(context: Context) {
        val settings = context.getSharedPreferences("de.beowulf.wetter", 0)
        val appWidgetAlarm = WidgetUpdater(context.applicationContext)
        var intervalMinutes: Long = settings.getInt("widget-update", 15).toLong()
        if(settings.getString("api", "") == context.getString(R.string.standardKey) && intervalMinutes < 60) {
            intervalMinutes = 60
        }
        appWidgetAlarm.startAlarm(intervalMinutes)
    }

    companion object {
        @JvmField
        var ACTION_AUTO_UPDATE: String = "AUTO_UPDATE"
    }
}