package com.example.personalphysicaltracker.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.personalphysicaltracker.data.model.Activity
import com.example.personalphysicaltracker.data.database.ActivityDao
import com.example.personalphysicaltracker.data.database.ActivityRoomDatabase

class ActivityRepository(app: Application) {

    var activityDao : ActivityDao

    init {
        val db = ActivityRoomDatabase.getDatabase(app)
        activityDao = db.ActivityDao()
    }


    fun saveActivity(activity: Activity) {
        Log.d("ActivityRepository", "saved Activity of type: ${activity.type}")
        Log.d("ActivityRepository", "saved Activity startime: ${activity.startTime}")
        Log.d("ActivityRepository", "saved Activity endtime: ${activity.endTime}")
        Log.d("ActivityRepository", "saved Activity duration: ${activity.duration}")
        Log.d("ActivityRepository", "saved Activity steps: ${activity.steps}")
        // Logica per salvare l'attività nel database
       ActivityRoomDatabase.databaseWriteExecutor.execute{
            activityDao.insertActivity(activity)
        }
    }

    fun getAllActivities(): LiveData<List<Activity>> {
        // Restituisce la lista osservabile delle attività
        return activityDao.getListOfActivities()
    }

    fun deleteActivity(toDelete: Activity) {
        ActivityRoomDatabase.databaseWriteExecutor.execute{
            activityDao.deleteActivity(toDelete)
        }
    }

    fun getDailySummary(startOfDay: Long, endOfDay: Long): LiveData<Long> {
        return activityDao.getDailyActivitySummary(startOfDay, endOfDay)
    }

    fun getDailySteps(startOfDay: Long, endOfDay: Long): Int {
        return activityDao.getDailySteps(startOfDay, endOfDay)
    }

    fun getActivitiesByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Activity>> {
        return activityDao.getActivitiesByDate(startOfDay, endOfDay)
    }

    fun getWeeklySummary(startOfWeek: Long, endOfWeek: Long): LiveData<Long> {
        return activityDao.getWeeklyActivitySummary(startOfWeek, endOfWeek)
    }

    fun getMonthlySummary(startOfMonth: Long, endOfMonth: Long): LiveData<Long> {
        return activityDao.getMonthlyActivitySummary(startOfMonth, endOfMonth)
    }

    fun getTotalDurationForTypeAndPeriod(type: String, fromDate: Long, toDate: Long): LiveData<Long> {
        return activityDao.getTotalDurationForTypeAndPeriod(type, fromDate, toDate)
    }

    fun getStepsForWeek(startOfWeek: Long, endOfWeek: Long): LiveData<List<Int>> {
        return activityDao.getStepsForWeek(startOfWeek, endOfWeek)
    }


}