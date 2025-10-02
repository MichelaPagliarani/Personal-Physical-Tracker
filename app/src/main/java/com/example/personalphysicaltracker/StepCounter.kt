package com.example.personalphysicaltracker

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * StepCounter is a utility class that uses the device's step counter sensor to track the number of steps
 * taken since the last device reboot. It provides methods to start and stop step tracking, and it notifies
 * a callback whenever the step count updates.
 *
 * - Uses SensorManager to access the step counter sensor.
 * - Registers a SensorEventListener to receive step count updates.
 * - Calls the provided lambda function with the updated step count.
 * - Allows stopping the sensor listener to save resources.
 */

class StepCounter (private val sensorManager: SensorManager) {

    val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var listener: SensorEventListener? = null

    // Variabile per memorizzare il valore dei passi dall'ultimo reboot
    private var stepsSinceReboot: Long = 0

    fun startCounting(onStepsUpdated: (Long) -> Unit) {
        if (sensor == null) {
            Log.w("StepCounter", "Step sensor not available")
            return
        }

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                // Ogni volta che il sensore rileva un cambiamento nei passi, aggiorna il conteggio
                stepsSinceReboot = event.values[0].toLong()
                Log.d("StepCounter", "Steps since last reboot: $stepsSinceReboot")
                onStepsUpdated(stepsSinceReboot)
            }

            // Notifica sul cambiamento di precisione del sensore
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.d("StepCounter", "Accuracy changed to: $accuracy")
            }
        }

        // Registra il listener per il sensore contapassi con una frequenza di aggiornamento adatta alla UI
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopCounting() {
        // Rimuove il listener per interrompere la registrazione dei passi e risparmiare batteria
        listener?.let {
            sensorManager.unregisterListener(it)
        }
        listener = null
    }


}