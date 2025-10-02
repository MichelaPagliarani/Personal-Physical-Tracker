package com.example.personalphysicaltracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.personalphysicaltracker.broadcast.DailyNotificationBroadcastReceiver
import java.util.Calendar

object AlarmDaily {
    fun scheduleRepeatingAlarm(context: Context) {


        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        //Setting alarm time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)


            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Creazione pending intent per il Broadcast Receiver
        val intent = Intent(context.applicationContext, DailyNotificationBroadcastReceiver::class.java).apply {
            action = "com.example.personalphysicaltracker.DAILY_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Settaggio repeating alarm
        alarmMgr.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Log.d("AlarmHelper", "Alarm set for: ${calendar.time}")
    }
}
