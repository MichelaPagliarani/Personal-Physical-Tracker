package com.example.personalphysicaltracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.personalphysicaltracker.data.model.Activity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for the HistoryActivity RecyclerView .
 * The adapter displays a list of activities, with the possibility to delete them.
 *
 * - Shows the date and time of the activity.
 * - Calculates and displays the duration of the activity.
 * - Allows deleting an activity with a bin button.
 *
 * Uses a callback to notify the removal of an activity to the calling component.
 */

class HistoryActivityAdapter(
    private var activities: MutableList<Activity>,
    private val context: Context,
    private val onDeleteActivity: (Activity) -> Unit // Callback per eliminare un'attività
) : RecyclerView.Adapter<HistoryActivityAdapter.ActivityViewHolder>() {

    // ViewHolder che contiene le viste per ogni elemento della lista
    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.textViewDate)
        val detailsText: TextView = view.findViewById(R.id.textViewActivityDetails)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteItemButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.activity_item, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]

        // Formattazione della data in dd-MM-yyyy
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val startDate = Date(activity.startTime)
        val endDate = activity.endTime?.let { Date(it) }

        // Formattazione dell'orario in HH:mm
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedStartTime = timeFormat.format(startDate)
        val formattedEndTime = endDate?.let { timeFormat.format(it) } ?: "N/A"

        // Imposto il testo della data
        val formattedDate = context.getString(R.string.date_text, dateFormat.format(startDate), formattedStartTime, formattedEndTime)
        holder.dateText.text = formattedDate

        // Calcolo della durata (da millisecondi a ore, minuti, secondi)
        val durationMillis = activity.duration ?: 0L
        val hours = (durationMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((durationMillis / (1000 * 60)) % 60).toInt()
        val seconds = ((durationMillis / 1000) % 60).toInt()

        // Imposto il testo dei dettagli
        val formattedDetails = context.getString(R.string.activityDetails, activity.type, hours, minutes,seconds)
        holder.detailsText.text = formattedDetails

        // Cancellazione dell'attività
        holder.deleteButton.setOnClickListener {
            onDeleteActivity(activity) // Chiamata alla callback con l'activity interessata
        }
    }

    override fun getItemCount(): Int = activities.size

    // Aggiorna la lista delle attività in modo efficiente usando DiffUtil.
    // Confronta la vecchia e la nuova lista senza ricaricare tutto e applica le modifiche solo agli elementi effettivamente cambiati
    fun updateActivities(newActivities: List<Activity>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = activities.size
            override fun getNewListSize(): Int = newActivities.size

            // Controllo se due attività sono la stessa (stesso ID)
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return activities[oldItemPosition].id == newActivities[newItemPosition].id
            }

            // Controllo se il contenuto dell'attività è cambiato
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return activities[oldItemPosition] == newActivities[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Aggiorno la lista solo dopo aver calcolato le differenze
        activities = newActivities.toMutableList()
        diffResult.dispatchUpdatesTo(this) // Aggiorno solo gli elementi necessari
    }

    // Rimuove un'activity dalla lista e notifica la RecyclerView.
    fun deleteActivity(activity: Activity) {
        val position = activities.indexOf(activity)
        if (position != -1) {
            activities.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, activities.size) // Mantiene la coerenza (indicizzazione) della lista
        }
    }

}