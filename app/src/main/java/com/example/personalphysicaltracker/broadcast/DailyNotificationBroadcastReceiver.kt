package com.example.personalphysicaltracker.broadcast

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.personalphysicaltracker.utils.DateUtils
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.data.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailyNotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DailyNotification", "BroadcastReceiver triggered")
        val action = intent.action
        Log.d("DailyNotification", "Received action: $action")
        CoroutineScope(Dispatchers.Default).launch {

            val steps = getDailySteps(context)

            val  text = if (steps > 2000) {
                "Today you walked $steps steps. Keep it up!"
            } else {
                "You walked only $steps steps. Walk more!"
            }

            when (action) {
                "com.example.personalphysicaltracker.DAILY_NOTIFICATION" -> {
                    Log.d("DailyNotification", "Correct action received, sending notification.")

                    // invio notifica
                    sendNotification(context, text)
                }
                else -> {
                    Log.w("DailyNotification", "Unexpected action received: $action")
                }
            }

        }

    }

    private suspend fun getDailySteps(context: Context) :Int{
        return withContext(Dispatchers.IO) {
            val currentMillis = System.currentTimeMillis()
            val startOfDay = DateUtils.getStartOfDayMillis(currentMillis)
            val endOfDay = DateUtils.getEndOfDayMillis(currentMillis)

            val activityRepository = ActivityRepository(context.applicationContext as Application)
            activityRepository.getDailySteps(startOfDay, endOfDay)
        }
    }

    private fun sendNotification(context: Context, text: String){
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dailyNotificationChannel"

        // Crea il canale di notifica (necessario per Android 8.0+)
        val channel = NotificationChannel(
            channelId,
            "Daily Notification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // Crea la notifica
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Daily Reminder")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_app_icon)
            .build()

        // Mostra la notifica
        notificationManager.notify(4, notification)
    }


}