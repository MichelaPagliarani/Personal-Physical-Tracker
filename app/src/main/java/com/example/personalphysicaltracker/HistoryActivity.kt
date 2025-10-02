package com.example.personalphysicaltracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalphysicaltracker.data.model.Activity
import com.example.personalphysicaltracker.data.repository.ActivityRepository
import com.example.personalphysicaltracker.utils.DateUtils
import java.util.Calendar

class HistoryActivity : AppCompatActivity() {

    private lateinit var activityRepository: ActivityRepository
    private lateinit var recyclerViewActivities: RecyclerView
    private lateinit var activityHistoryAdapter: HistoryActivityAdapter
    private lateinit var buttonSelectDate: Button
    private lateinit var buttonViewAll: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recyclerViewActivities)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        activityRepository = ActivityRepository(application)

        recyclerViewActivities = findViewById(R.id.recyclerViewActivities)
        recyclerViewActivities.layoutManager = LinearLayoutManager(this)

        // Configura l'adattatore con la callback
        activityHistoryAdapter = HistoryActivityAdapter(mutableListOf(), this) { activity ->
            deleteActivity(activity) // Callback per l'eliminazione
        }

        //activityHistoryAdapter = HistoryActivityAdapter(mutableListOf(), this)
        recyclerViewActivities.adapter = activityHistoryAdapter

        buttonSelectDate = findViewById(R.id.buttonSelectDate)
        buttonViewAll = findViewById(R.id.buttonViewAll)

        buttonSelectDate.setOnClickListener {
            showDatePicker()
        }
        buttonViewAll.setOnClickListener {
            loadAllActivities()
        }

        // Update the mutable list with GetAllActivities from repository
        activityRepository.getAllActivities().observe(this) { activities ->
            activityHistoryAdapter.updateActivities(activities)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.timeInMillis
                loadActivitiesByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun loadActivitiesByDate(dateMillis: Long) {
        val startOfDay = DateUtils.getStartOfDayMillis(dateMillis)
        val endOfDay = DateUtils.getEndOfDayMillis(dateMillis)

        activityRepository.getActivitiesByDate(startOfDay, endOfDay).observe(this) { activities ->
            activityHistoryAdapter.updateActivities(activities)
        }
    }

    private fun loadAllActivities() {
        activityRepository.getAllActivities().observe(this) { activities ->
            activityHistoryAdapter.updateActivities(activities)
        }
    }

    private fun deleteActivity(activity: Activity) {
        activityRepository.deleteActivity(activity)
        activityHistoryAdapter.deleteActivity(activity)
        Toast.makeText(this, "${activity.type} deleted", Toast.LENGTH_SHORT).show()
    }


}