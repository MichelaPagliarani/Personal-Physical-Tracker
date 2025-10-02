package com.example.personalphysicaltracker.preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val CURRENT_ACTIVITY_TYPE = stringPreferencesKey("current_activity_type")
val IS_ACTIVITY_RUNNING = booleanPreferencesKey("is_activity_running")
val CURRENT_ACTIVITY_START_TIME = longPreferencesKey("current_activity__start_time")
val USER_PRIORITY = booleanPreferencesKey("user_priority")
val USER_STEPS = longPreferencesKey("user_steps")
val IS_RECOGNITION_ACTIVE = booleanPreferencesKey("is_recognition_active")


