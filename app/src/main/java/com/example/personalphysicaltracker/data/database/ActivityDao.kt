package com.example.personalphysicaltracker.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.personalphysicaltracker.data.model.Activity

@Dao
interface ActivityDao {

    @Query("SELECT * FROM Activity ORDER BY startTime DESC")
    fun getListOfActivities(): LiveData<List<Activity>>

    @Insert
    fun insertActivity(activity: Activity)

    @Delete
    fun deleteActivity(activity: Activity)

    // Query per ottenere l'attività totale di un giorno specifico
    @Query("SELECT SUM(duration) FROM activity WHERE startTime >= :startOfDay AND endTime < :endOfDay")
    fun getDailyActivitySummary(startOfDay: Long, endOfDay: Long): LiveData<Long>

    @Query("SELECT SUM(steps) FROM activity WHERE startTime >= :startOfDay AND endTime < :endOfDay")
    fun getDailySteps(startOfDay: Long, endOfDay: Long): Int


    @Query("SELECT * FROM activity WHERE startTime >= :startOfDay AND endTime < :endOfDay")
    fun getActivitiesByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Activity>>


    // Query per ottenere l'attività totale in una settimana
    @Query("SELECT SUM(duration) FROM activity WHERE startTime >= :startOfWeek AND endTime < :endOfWeek")
    fun getWeeklyActivitySummary(startOfWeek: Long, endOfWeek: Long): LiveData<Long>

    // Query per ottenere l'attività totale in un mese
    @Query("SELECT SUM(duration) FROM activity WHERE startTime >= :startOfMonth AND endTime < :endOfMonth")
    fun getMonthlyActivitySummary(startOfMonth: Long, endOfMonth: Long): LiveData<Long>

    @Query(
        """
    SELECT SUM(duration) as totalDuration 
    FROM activity 
    WHERE type = :type AND startTime >= :fromDate AND startTime < :toDate
"""
    )
    fun getTotalDurationForTypeAndPeriod(type: String, fromDate: Long, toDate: Long): LiveData<Long>

    @Query("""
    SELECT SUM(steps) FROM activity
    WHERE startTime >= :startOfWeek AND startTime < :endOfWeek
    GROUP BY strftime('%w', datetime(startTime / 1000, 'unixepoch', 'localtime'))
""")
    fun getStepsForWeek(startOfWeek: Long, endOfWeek: Long): LiveData<List<Int>>


}