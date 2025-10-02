package com.example.personalphysicaltracker

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import com.example.personalphysicaltracker.data.model.ActivityType

class ChangeActivityDialog(context: Context, private val onActivitySelected: (String) -> Unit) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_activity)

        val listView = findViewById<ListView>(R.id.listViewActivity)
        listView.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_1,
            ActivityType.entries.toTypedArray()
        )

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedActivity = ActivityType.entries[position].name
            onActivitySelected(selectedActivity)
            dismiss()
        }
    }
}
