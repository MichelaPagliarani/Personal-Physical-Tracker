package com.example.personalphysicaltracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.personalphysicaltracker.data.model.Activity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [Activity::class], version = 1, exportSchema = false)
abstract class ActivityRoomDatabase : RoomDatabase() {

    //funzione per ottenere riferimento a ActivityDao
    abstract fun ActivityDao(): ActivityDao

    companion object{

        //per fare un singleton in maniera sicura
        //roomdb non ha costruttore, ogni volta per riferimento richiamare getDatabase

        @Volatile
        private var INSTANCE: ActivityRoomDatabase? = null

        private const val nThreads: Int = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(nThreads)

        private val sRoomDatabaseCallback = object: RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                /* What happens when the database gets called for the first time? */
                databaseWriteExecutor.execute() {
}
            }
        }

        fun getDatabase(context: Context) : ActivityRoomDatabase {
            return INSTANCE ?: synchronized (this) {
                val _INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    ActivityRoomDatabase::class.java,
                    "activity_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(sRoomDatabaseCallback)//optional
                    .build()
                INSTANCE = _INSTANCE
                _INSTANCE
            }
        }
    }
}