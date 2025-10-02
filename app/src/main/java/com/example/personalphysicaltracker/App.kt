package com.example.personalphysicaltracker

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.personalphysicaltracker.preferences.CURRENT_ACTIVITY_TYPE
import com.example.personalphysicaltracker.preferences.IS_ACTIVITY_RUNNING
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


// Crea una proprietà di DataStore a livello di contesto
val Context.dataStore by preferencesDataStore(name = "currentActivityData")
val DEFAULT_CURRENT_ACTIVITY_TYPE = "Unknown"
val DEFAULT_IS_ACTIVITY_RUNNING = false

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inizializzazioni generali se necessarie valgono fino a che app chiusa con onTerminate()
    }

    private fun setDefaultValues() {
        runBlocking {
            val InitialCurrentActivityData = dataStore.data.first()
            val currentActivityType = InitialCurrentActivityData[CURRENT_ACTIVITY_TYPE]
            val isActivityRunning = InitialCurrentActivityData[IS_ACTIVITY_RUNNING]
            if (currentActivityType == null || isActivityRunning == null) {
                dataStore.edit { settings ->
                    if (currentActivityType == null) { settings[CURRENT_ACTIVITY_TYPE] = DEFAULT_CURRENT_ACTIVITY_TYPE }
                    if (isActivityRunning == null) { settings[IS_ACTIVITY_RUNNING] = DEFAULT_IS_ACTIVITY_RUNNING }
                }
            }
        }
    }
}
