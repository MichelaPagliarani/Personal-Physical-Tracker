package com.example.personalphysicaltracker.broadcast
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.personalphysicaltracker.ActivityChronometerService
import com.example.personalphysicaltracker.data.model.ActivityType
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient


class ActivityTransitionBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ActivityTransitionBroadcastReceiver"
        private lateinit var activityRecognitionClient: ActivityRecognitionClient

        fun startActivityTransitionUpdates(context: Context) {

            activityRecognitionClient = ActivityRecognition.getClient(context)

            val transitions = listOf(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )

            val request = ActivityTransitionRequest(transitions)
            Log.d(TAG,"request: $request")

            /** Creazione PendingIntent */
           val intent = Intent(context, ActivityTransitionBroadcastReceiver::class.java).apply {
                action = "com.google.android.gms.location.ACTIVITY_TRANSITION"
            }

            var pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,  // l'intent con l'azione impostata
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )
            if (pendingIntent != null) {
                Log.d(TAG, "Already existing PendingIntent, could generate conflicts")
            } else {
                Log.d(TAG, "No PendingIntent found, creating a new one")
                pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                )
            }

            Log.d(TAG,"pending intent: $pendingIntent")


            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing activity recognition permission")
                return
            }

            // Richiedi aggiornamenti utilizzando ActivityRecognitionClient
            activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successful activity recognition registration")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "UNsuccessful activity recognition registration: ${e.message}")
                }
        }


        fun stopActivityTransitionUpdates(context: Context) {
            Log.d(TAG, "Stopping activity transition updates")

            val intent = Intent(context, ActivityTransitionBroadcastReceiver::class.java).apply {
                action = "com.google.android.gms.location.ACTIVITY_TRANSITION"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing activity recognition permission")
                return
            }

            activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully unregistered from activity transition updates")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unregister from activity transition updates: ${e.message}")
                }
        }

    }


    override fun onReceive(context: Context, intent: Intent) {

        Log.d(TAG, "Received Intent with Activity")

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)

            result?.transitionEvents?.forEach { event ->

                val activityType = when (event.activityType) {
                    DetectedActivity.IN_VEHICLE -> ActivityType.DRIVING
                    DetectedActivity.WALKING -> ActivityType.WALKING
                    DetectedActivity.STILL -> ActivityType.SITTING
                    else -> ActivityType.UNKNOWN
                }

                val transitionType = when (event.transitionType) {
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "Started"
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "Stopped"
                    else -> "Sconosciuto"
                }

                Log.d(TAG, "Transition detected: $activityType ($transitionType)")

                /**Start/Stop activity**/
                val isEntering = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
                Log.d(TAG, "Sending intent to Service with $activityType and $isEntering")
                // Invia l'evento al Service
                val serviceIntent = Intent(context, ActivityChronometerService::class.java).apply {
                    putExtra("activity_type", activityType)
                    putExtra("is_entering", isEntering)

                }
                ContextCompat.startForegroundService(context, serviceIntent)
                //context.startService(serviceIntent)

                val message = "User $transitionType $activityType"
                Log.d(TAG, message)
            }
        } else {
            Log.d(TAG, "No activity in the Intent")
        }
    }




}
