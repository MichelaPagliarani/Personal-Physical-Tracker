/**ViewModel che si riferirà al ActivityRepository**/
package com.example.personalphysicaltracker

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.data.model.Activity
import com.example.personalphysicaltracker.data.model.ActivityType
import com.example.personalphysicaltracker.data.repository.ActivityRepository
import com.example.personalphysicaltracker.preferences.DataStorePreferencesManager
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch



class ActivityViewModel(private val repository: ActivityRepository, context: Context) : ViewModel() {

    // Flow osservabili provenienti dal DataStore
    val isActivityRunning: LiveData<Boolean> = DataStorePreferencesManager
        .readIsActivityRunningFlow(context)
        .asLiveData()

    val currentActivityType: LiveData<ActivityType> = DataStorePreferencesManager
        .readCurrentActivityTypeFlow(context)
        .asLiveData()

    // Variabili per gestire l'attività fisica
    //oggetto Activity che mantiene lo stato dell'attività fisica corrente
    private var currentActivity: Activity? = null

    fun updateActivityState(context: Context, isRunning: Boolean) {
        DataStorePreferencesManager.saveIsActivityRunning(context, isRunning)
    }

    fun updateCurrentActivityType(context: Context, activityType: ActivityType) {
        DataStorePreferencesManager.saveCurrentActivityType(context, activityType)
    }

    fun updateUserPriority(context: Context, userPriority: Boolean) {
        DataStorePreferencesManager.setUserPriority(context, userPriority)
    }


    /** Metodi di interazione */
    // Cambio manuale dell'attività
    fun changeActivity(activity: ActivityType, context: Context, activityStartTime: Long, steps: Long) {
        if(isActivityRunning.value == true){
            stopActivity(context, activityStartTime, steps)
        }
        updateCurrentActivityType(context, activity)
        updateActivityState(context, false)
    }

    //Avvia lo stato dell'attività
    fun startCurrentActivity(context: Context){
        updateActivityState(context, true)
   }

    //Ferma l'attività corrente, aggiornando il tempo di fine e salvando l'attività.
    /* viewModelScope è una proprietà predefinita del ViewModel in Android che ti permette di lanciare coroutine in modo sicuro. Fa parte delle librerie Jetpack e fa sì che le coroutine siano legate al ciclo di vita del ViewModel*/
    /*launch è una funzione di estensione che crea una nuova coroutine e la esegue nel contesto del CoroutineScope, che in questo caso è viewModelScope*/
    fun stopActivity(context: Context, currentActivityStartTime: Long, steps: Long) { /**cambio manuale dell'attività**/
        val endTime = System.currentTimeMillis()
        val startTime = currentActivityStartTime
        val duration = endTime - startTime
        currentActivity = currentActivityType.value?.let {
            Activity(
                type = it,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                steps = steps
            )
        }

        currentActivity?.duration = currentActivity?.endTime?.minus(currentActivity!!.startTime)

        /*Attention, since this coroutine is started with viewModelScope, it is executed in the scope of the ViewModel.
        If the ViewModel is destroyed because the user is navigating away from the screen,
        viewModelScope is automatically cancelled, and all running coroutines are canceled as well.*/
        currentActivity?.let { activity ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveActivity(activity) // Salva l'attività nel repository
            }
        }
        updateActivityState(context, false)
        updateUserPriority(context, false)
        currentActivity = null
    }

    /**Metodo invocato quando l'utente clicca il bottone di start/stop**/
    fun toggleActivityState(context: Context, activityStartTime: Long, steps: Long) {
        if (isActivityRunning.value == true) {
            Log.d("toggleActivityState","isActRun è true, richiamo stopActivity")
            stopActivity(context, activityStartTime, steps) //diventa false
        } else {
            Log.d("toggleActivityState","isActRun è true, richiamo startCurrentActivity")
            startCurrentActivity(context) //diventa true
        }
    }


}


class ActivityViewModelFactory(private val repository: ActivityRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

