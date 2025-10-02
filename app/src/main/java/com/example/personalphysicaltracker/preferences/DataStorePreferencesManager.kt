package com.example.personalphysicaltracker.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.personalphysicaltracker.data.model.ActivityType
import com.example.personalphysicaltracker.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**DataStore associato al contesto dell'applicazione. Dati accessibili fino a che app installata**/
object DataStorePreferencesManager {

    // Funzione per salvare i dati in DataStore
    fun saveIsActivityRunning(context: Context, isActivityRunning: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { currentActivityData ->
                currentActivityData[IS_ACTIVITY_RUNNING] = isActivityRunning
            }
        }
    }

    fun saveCurrentActivityType(context: Context, currentActivity: ActivityType) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { currentActivityData ->
                currentActivityData[CURRENT_ACTIVITY_TYPE] = currentActivity.Name
            }
        }
    }

    fun saveCurrentActivityStartTime(context: Context, currentActivityStartTime: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { currentActivityData ->
                currentActivityData[CURRENT_ACTIVITY_START_TIME] = currentActivityStartTime
            }
        }
    }

    fun setUserPriority(context: Context, value: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { preferences ->
                preferences[USER_PRIORITY] = value
            }
        }
    }

    fun setIsRecognitionActive(context: Context, value: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { preferences ->
                preferences[IS_RECOGNITION_ACTIVE] = value
            }
        }
    }

    fun readIsActivityRunningFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { currentActivityData ->
                currentActivityData[IS_ACTIVITY_RUNNING] ?: false
            }
    }

    fun readCurrentActivityTypeFlow(context: Context): Flow<ActivityType> {
        return context.dataStore.data
            .map { currentActivityData ->
                val activityTypeString = currentActivityData[CURRENT_ACTIVITY_TYPE] ?: "Unknown"
                ActivityType.entries.find { it.Name == activityTypeString } ?: ActivityType.UNKNOWN
            }
    }

    fun readUserPriority(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { currentActivityData ->
                currentActivityData[USER_PRIORITY] ?: false
            }
    }

    fun readIsRecognitionActive(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { currentActivityData ->
                currentActivityData[IS_RECOGNITION_ACTIVE] ?: false
            }
    }

    fun readCurrentActivityStartTimeFlow(context: Context): Flow<Long> {
        return context.dataStore.data
            .map { currentActivityData ->
                currentActivityData[CURRENT_ACTIVITY_START_TIME] ?: 0
            }
    }

}