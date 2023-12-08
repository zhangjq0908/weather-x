package de.beowulf.wetter.widget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

@SuppressLint("UnspecifiedImmutableFlag")
class WidgetUpdater(private val mContext: Context) {
    private val alarmID = 0
    fun startAlarm(intervalMinutes: Long) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, intervalMinutes.toInt())
        val alarmIntent = Intent(mContext, WidgetProvider::class.java)
        alarmIntent.action = WidgetProvider.ACTION_AUTO_UPDATE
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                mContext,
                alarmID,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                mContext,
                alarmID,
                alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        }
        val alarmManager = mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        // RTC does not wake the device up
        alarmManager.setRepeating(
            AlarmManager.RTC,
            calendar.timeInMillis,
            intervalMinutes * 60_000,
            pendingIntent
        )
    }

    fun stopAlarm() {
        val alarmIntent = Intent(mContext, WidgetProvider::class.java)
        alarmIntent.action = WidgetProvider.ACTION_AUTO_UPDATE
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                mContext,
                alarmID,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                mContext,
                alarmID,
                alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        }
        val alarmManager = mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}