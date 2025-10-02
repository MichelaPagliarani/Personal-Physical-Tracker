package com.example.personalphysicaltracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.personalphysicaltracker.broadcast.ActivityTransitionBroadcastReceiver
import com.example.personalphysicaltracker.data.model.Activity
import com.example.personalphysicaltracker.data.model.ActivityType
import com.example.personalphysicaltracker.data.repository.ActivityRepository
import com.example.personalphysicaltracker.preferences.DataStorePreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.Locale

class ActivityChronometerService : Service(){

    private lateinit var notificationManager: NotificationManagerCompat
    private val binder = LocalBinder()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val mutex = Mutex()  // Mutex per sincronizzare l’accesso ai dati


    /**Chronometer variables**/
    //TODO cambio qui, tolto il private a startTime
    /*private*/ var startTime: Long = 0
    // LiveData per monitorare il tempo trascorso in millisecondi
    private val _elapsedTime = MutableLiveData<Long>()
    val elapsedTime: LiveData<Long> get() = _elapsedTime
    private var chronometerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)


    /**Activity Tracker variables**/
    private val _isRecognitionActive = MutableLiveData(false)
    val isRecognitionActive: LiveData<Boolean> get() = _isRecognitionActive
    private val repository: ActivityRepository by lazy {
        ActivityRepository(application) // Passa l'application context se necessario
    }

    /**Steps variables**/
    private lateinit var sensorManager: SensorManager
    private var stepCounter: StepCounter? = null
    private val _stepsSinceStart = MutableLiveData<Long>()
    val stepsSinceStart: LiveData<Long> get() = _stepsSinceStart
    private var initialStepCount = 0L
    private val _isWalking = MutableStateFlow(false)
    val isWalking: StateFlow<Boolean> get() = _isWalking


    inner class LocalBinder : Binder() {
        fun getService(): ActivityChronometerService = this@ActivityChronometerService
    }

    companion object {
        const val CHANNEL_ID = "ChronometerServiceChannel"
        const val NOTIFICATION_ID = 1
        // Costanti per le azioni
        const val ACTION_START_ACTIVITY_RECOGNITION = "com.example.personalphysicaltracker.ACTION_START_ACTIVITY_RECOGNITION"
        const val ACTION_STOP_ACTIVITY_RECOGNITION = "com.example.personalphysicaltracker.ACTION_STOP_ACTIVITY_RECOGNITION"
        const val ACTION_START_ACTIVITY_MANUAL = "com.example.personalphysicaltracker.ACTION_START_ACTIVITY_MANUAL"
        const val ACTION_STOP_ACTIVITY_MANUAL = "com.example.personalphysicaltracker.ACTION_STOP_ACTIVITY_MANUAL"

    }

    /**Service creation, NotificationManager setup**/
    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()

        // Configura il SensorManager e StepCounter
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounter = StepCounter(sensorManager)

        // Avvia una coroutine per osservare i cambiamenti di attività
        coroutineScope.launch {
            DataStorePreferencesManager.readCurrentActivityTypeFlow(this@ActivityChronometerService)
                .collect { activityType ->
                    _isWalking.value = (activityType == ActivityType.WALKING)
                }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val isActive = DataStorePreferencesManager.readIsRecognitionActive(applicationContext).first()
            _isRecognitionActive.postValue(isActive)
        }

        Log.d("new","leggo isRecognitionactive ${_isRecognitionActive.value}")
    }


    // Eseguito quando il Service viene avviato
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Inizia subito in modalità foreground per evitare l'ANR
        startForeground(NOTIFICATION_ID, createNotification("Initializing activity tracking"))


        when (intent?.action) {
            ACTION_START_ACTIVITY_RECOGNITION -> {
                userStartRecognition()      // Avvia la ricognizione
            }
            ACTION_STOP_ACTIVITY_RECOGNITION -> {
                userStopRecognition()       // Ferma la ricognizione
                stopActivityForRecognition()

                return START_NOT_STICKY
            }
            ACTION_START_ACTIVITY_MANUAL -> {
                startChronometer()      // Avvia solo il cronometro in modalità manuale
            }
            ACTION_STOP_ACTIVITY_MANUAL -> {
                stopChronometer()       // Ferma il cronometro in modalità manuale
                return START_NOT_STICKY
            }
        }

        val activityType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra("activity_type", ActivityType::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra("activity_type") as? ActivityType
        }

        val isEntering = intent?.getBooleanExtra("is_entering", false) ?: false

        // Avvio o arresto dell’attività, sincronizzato con il Mutex
        coroutineScope.launch {
            mutex.withLock {
                if (activityType != null) {
                    if (isEntering) {
                        delay(150)
                        startActivityForRecognition(activityType)
                        startChronometer()
                    } else {
                       stopActivityForRecognition()
                    }
                } else {
                    Log.d("ActivityChronometerService", "activityType is null or invalid")
                }
            }
        }

        return START_STICKY
    }

    private fun startActivityForRecognition(activityType: ActivityType) {
        coroutineScope.launch {
            mutex.withLock {
                DataStorePreferencesManager.saveCurrentActivityType(
                    applicationContext,
                    activityType
                )
                DataStorePreferencesManager.saveCurrentActivityStartTime(
                    applicationContext,
                    System.currentTimeMillis()
                )
            }
        }

    }


    fun stopActivityForRecognition() {
        val stepsDone = getStepsSinceStart()
        stopChronometer()
        coroutineScope.launch {
            mutex.withLock {
                // Ottieni i dati direttamente da DataStore per evitare la latenza di LiveData
                val startTime = DataStorePreferencesManager.readCurrentActivityStartTimeFlow(this@ActivityChronometerService).firstOrNull()
                val activityType = DataStorePreferencesManager.readCurrentActivityTypeFlow(this@ActivityChronometerService).firstOrNull()
                Log.d("bro2","$activityType ora subito prima creare ogg")

                if (startTime != null && activityType != null) {
                    // Calcola la durata e crea l'oggetto Activity
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    val activity = Activity(
                        type = activityType,
                        startTime = startTime,
                        endTime = endTime,
                        duration = duration,
                        steps = stepsDone
                    )
                    coroutineScope.launch(Dispatchers.IO) {
                        repository.saveActivity(activity)
                    }
                } else {
                    Log.e(
                        "ActivityChronometerService",
                        "Valori di startTime o activityType non disponibili"
                    )
                }
            }
        }
    }

    /**Last method called when destruction is coming**/
    //mi assicuro che tutte le corutine vengano cancellate
    override fun onDestroy() {
        serviceScope.cancel()
        coroutineScope.cancel()
        chronometerJob?.cancel()
        chronometerJob = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    /**Notification implementation**/
    /*crea un canale per le notifiche, richiesto per i dispositivi Android 8.0 (Oreo) o superiori*/
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Chronometer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Activity Chronometer Notification Service Channel"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /*creazione notifica tempo trascorso*/
    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)//TODO
            .setContentTitle("Activity Started")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun updateNotification(formattedTimeString: String, steps: Long) {
        val message = if (_isWalking.value) {
            "Elapsed time: $formattedTimeString, Steps: $steps"
        } else {
            "Elapsed time: $formattedTimeString"
        }
        val notification = createNotification(message)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /** Metodo per avviare il monitoraggio attività. switch posto a true **/
    fun userStartRecognition() {
        Log.d("userStartedRecognition", "isrec $_isRecognitionActive")
        if (_isRecognitionActive.value == false) {

            ActivityTransitionBroadcastReceiver.startActivityTransitionUpdates(applicationContext)
            _isRecognitionActive.postValue(true)

            CoroutineScope(Dispatchers.IO).launch {
                DataStorePreferencesManager.setIsRecognitionActive(applicationContext, true)
            }
        }
    }



    /**Chronometer implementation**/
    /*
    Avvia il cronometro se non è già in esecuzione.
    Crea una coroutine che aggiorna il LiveData _elapsedTime ogni secondo, calcolando il tempo trascorso
    Aggiornamento notifica ogni secondo con tempo trascorso
    Activity aggionerà la UI del cronometro ricevendo l'aggiornamento del LiveData
    startForeground Per garantire che il servizio rimanga attivo anche in background (per visualizzare una notifica persistente e tenere il servizio in foreground.)
    */
    fun startChronometer() {
        Log.d("startchr", "entro in startChronometer")
        if (chronometerJob == null) {
            startTime = System.currentTimeMillis()
            _stepsSinceStart.postValue(0L)
            initialStepCount = 0L

            startForeground(NOTIFICATION_ID, createNotification("Timer started"))

            chronometerJob = serviceScope.launch {

                // Ascolta il cambiamento di stato di isWalking
                launch {
                    isWalking.collect { walking ->
                        if (walking) {
                            startStepCounting()
                        } else {
                            stopStepCounting()
                        }
                    }
                }

                while (isActive) {
                    val elapsedMillis = System.currentTimeMillis() - startTime
                    _elapsedTime.postValue(elapsedMillis)  // Aggiorna LiveData
                    val seconds = (elapsedMillis / 1000) % 60
                    val minutes = (elapsedMillis / 1000 / 60) % 60
                    val hours = (elapsedMillis / 1000 / 3600) % 24

                    //Locale:trasformazione coerente delle stringhe, indipendentemente dalle impostazioni locali dell'utente
                    val formattedTime = String.format(Locale.ROOT,"%02d:%02d:%02d", hours, minutes, seconds)
                    Log.d("ChronometerService", "Elapsed Time: $formattedTime")

                    val steps = getStepsSinceStart()
                    updateNotification(formattedTime, steps)

                    delay(1000)
                }
                Log.d("ChronometerService", "Cronometro coroutine terminata")
            }
        }
    }


    private fun startStepCounting() {
        //every time sensor updates total steps, the value is passed to the lambda and
        //assigned to stepsSinceReboot
        stepCounter?.startCounting { stepsSinceReboot ->
            //lambda that defines what happens every time the sensor receives updates
            if (initialStepCount == 0L) {
                initialStepCount = stepsSinceReboot
            }
            val currentSteps = stepsSinceReboot - initialStepCount
            _stepsSinceStart.postValue(currentSteps)  // Aggiorna LiveData con il nuovo valore dei passi
            Log.d("StepCounter", "Passi aggiornati relativi all'attività corrente: $currentSteps")
        }
    }

    fun getStepsSinceStart(): Long {
        return stepsSinceStart.value ?: 0L
    }

    private fun stopStepCounting() {
        stepCounter?.stopCounting()
    }


    fun stopChronometer() {
        chronometerJob?.cancel()  // Cancella il CoroutineScope del cronometro
        chronometerJob = null  // Reset per permettere future esecuzioni

        Log.d("ChronometerService", "Cronometro fermato e coroutine annullata: $chronometerJob")

        // Termina il servizio se la ricognizione è disabilitata
        if (_isRecognitionActive.value == false) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.d("ChronometerService", "Servizio terminato completamente")

            // Reset dei passi quando si ferma l'attività
            stopStepCounting()
            initialStepCount = 0L // Resetta il valore iniziale
        }
    }


    fun userStopRecognition() {

        if (_isRecognitionActive.value == true) {
            ActivityTransitionBroadcastReceiver.stopActivityTransitionUpdates(applicationContext)
            //use value instead of postValue (postValue doesn't work totally here)
            _isRecognitionActive.value = false
            CoroutineScope(Dispatchers.IO).launch {
                DataStorePreferencesManager.setIsRecognitionActive(applicationContext, false)
            }
            Log.d("ChronometerService", "Ricognizione attività fermata, isrec ${_isRecognitionActive.value}")

        }
    }


}
