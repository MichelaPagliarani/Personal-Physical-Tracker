package com.example.personalphysicaltracker.broadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.personalphysicaltracker.AlarmDaily

class BootReceiver : BroadcastReceiver() {
    /**Set the alarm when the device is rebooted**/
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmDaily.scheduleRepeatingAlarm(context)
        }
    }
}
