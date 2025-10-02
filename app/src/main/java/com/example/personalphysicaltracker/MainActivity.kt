package com.example.personalphysicaltracker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.airbnb.lottie.LottieAnimationView
import android.Manifest
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.example.personalphysicaltracker.data.model.ActivityType
import com.example.personalphysicaltracker.data.repository.ActivityRepository

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACTIVITY = "activity"
    }

    private lateinit var labelCurrentActivity : TextView
    private lateinit var animationViewCurrentActivity: LottieAnimationView
    private lateinit var activityChronometer : Chronometer
    private lateinit var switchActivityRecognition : SwitchCompat
    private var isTrackingEnabled = false

    /**Permissions**/
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Controlla la risposta dell'utente per ogni permesso richiesto
        permissions.entries.forEach { permission ->
            when (permission.key) {
                Manifest.permission.POST_NOTIFICATIONS -> {
                    if (permission.value) {
                        // Permesso per le notifiche concesso
                        Toast.makeText(this, "Permesso per le notifiche concesso", Toast.LENGTH_SHORT).show()
                    } else {
                        // Permesso per le notifiche negato
                        Toast.makeText(this, "Permesso per le notifiche negato. Le notifiche non saranno abilitate.", Toast.LENGTH_LONG).show()
                    }
                }
                Manifest.permission.ACTIVITY_RECOGNITION -> {
                    if (permission.value) {
                        // Permesso per il riconoscimento attività concesso
                        Toast.makeText(this, "Permesso per il riconoscimento delle attività concesso", Toast.LENGTH_SHORT).show()
                    } else {
                        // Permesso per il riconoscimento attività negato
                        Toast.makeText(this, "Permesso per il riconoscimento delle attività negato. Alcune funzionalità potrebbero non funzionare.", Toast.LENGTH_LONG).show()
                    }
                }
                Manifest.permission.BODY_SENSORS -> {
                    if (permission.value) {
                        // Permesso per il riconoscimento attività concesso
                        Toast.makeText(this, "Permesso per i body sensors concesso", Toast.LENGTH_SHORT).show()
                    } else {
                        // Permesso per il riconoscimento attività negato
                        Toast.makeText(this, "Permesso per i body sensors negato. Alcune funzionalità potrebbero non funzionare.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    /**Gestione Service cronometro*/
    private lateinit var chronometerService: ActivityChronometerService
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ActivityChronometerService.LocalBinder
            chronometerService = binder.getService()
            bound = true

            // Osserva il LiveData elapsedTime per aggiornare il Chronometer
            chronometerService.elapsedTime.observe(this@MainActivity) { elapsedMillis ->
                activityChronometer.base = SystemClock.elapsedRealtime() - elapsedMillis
            }

            // Observer che osserva il LiveData stepsSinceStart per aggiornare la visualizzazione del numero di passi
            chronometerService.stepsSinceStart.observe(this@MainActivity) { stepsText ->
                findViewById<TextView>(R.id.stepsText).text = "Steps: $stepsText"
            }

            chronometerService.isRecognitionActive.observe(this@MainActivity) { isActive  ->
                switchActivityRecognition.isChecked = isActive
            }

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.txtHello)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        /**Check permissions*/
        checkAndRequestPermissions()


        /**View Model**/
        val activityViewModel: ActivityViewModel by viewModels {
            ActivityViewModelFactory(
                ActivityRepository(application),
                application.applicationContext
            )
        }


        /**UI elements**/
        labelCurrentActivity = findViewById(R.id.currentActivity)
        animationViewCurrentActivity = findViewById<LottieAnimationView>(R.id.animationView)
        activityChronometer = findViewById<Chronometer>(R.id.chronometer)


        /** START/STOP CURRENT ACTIVITY**/
        val buttonStartStop: Button = findViewById(R.id.buttonStartStop)
        val buttonChange: Button = findViewById(R.id.buttonChange)

        // Osserva i cambiamenti di currentActivity
        activityViewModel.currentActivityType.observe(this, Observer { activity ->
            labelCurrentActivity.text = activity.Name
            loadAnimation(activity.name.lowercase())
        })

        // Osserva i cambiamenti di isActivityRunning
        activityViewModel.isActivityRunning.observe(this, Observer { isRunning ->
            if (isRunning) animationViewCurrentActivity.playAnimation()
            else animationViewCurrentActivity.pauseAnimation()
        })



        buttonStartStop.setOnClickListener {
            val steps = chronometerService.getStepsSinceStart()
            if (activityViewModel.isActivityRunning.value == true   ) {
                sendServiceAction(ActivityChronometerService.ACTION_STOP_ACTIVITY_MANUAL)
                activityChronometer.stop()
            } else {
                startChronometerService()
                sendServiceAction(ActivityChronometerService.ACTION_START_ACTIVITY_MANUAL)
                activityChronometer.start()
            }
            activityViewModel.updateUserPriority(application.applicationContext, true)
            activityViewModel.toggleActivityState(application.applicationContext, chronometerService.startTime, steps)

            Toast.makeText(this, "Start/Stop clicked", Toast.LENGTH_SHORT).show()
        }


        /**Start/Stop Activity Recognition**/
        switchActivityRecognition = findViewById<SwitchCompat>(R.id.switchActivityRecognition)

        switchActivityRecognition.setOnCheckedChangeListener { _, isChecked ->
            isTrackingEnabled = isChecked
            if (isTrackingEnabled) {
                sendServiceAction(ActivityChronometerService.ACTION_START_ACTIVITY_RECOGNITION)
            } else {
                // Verifica che il tracking sia effettivamente attivo prima di tentare di fermarlo
                if (chronometerService.isRecognitionActive.value == true) {
                    sendServiceAction(ActivityChronometerService.ACTION_STOP_ACTIVITY_RECOGNITION)
                }
            }
        }


        /**Change Activity**/
        buttonChange.setOnClickListener {
            val steps = chronometerService.getStepsSinceStart()
            val dialog = ChangeActivityDialog(this) { selectedActivity ->
                activityViewModel.changeActivity(ActivityType.valueOf(selectedActivity), application.applicationContext, chronometerService.startTime, steps)
            }
            dialog.show()
        }


        /**History**/
        val buttonHistory: Button = findViewById(R.id.buttonHistory)
        buttonHistory.setOnClickListener{
            val historyIntent = Intent(this, HistoryActivity::class.java)
            startActivity(historyIntent)
        }


        /**Statistics**/
        val buttonStatistics: Button = findViewById(R.id.buttonStatistics)
        buttonStatistics.setOnClickListener{
            val statisticsIntent = Intent(this, StatisticsActivity::class.java)
            startActivity(statisticsIntent)
        }

        /**Calling the scheduler for the daily notification**/
        AlarmDaily.scheduleRepeatingAlarm(applicationContext)

    }


    /**Activity Animation**/
    private fun loadAnimation(animationName: String) {
        val animationMap = mapOf(
            "driving" to R.raw.driving,
            "walking" to R.raw.walking,
            "sitting" to R.raw.sitting
        )

        //Recupero l'ID della risorsa dalla mappa e se esiste carico l'animazione
        val resId = animationMap[animationName]

        if (resId != null) {
            animationViewCurrentActivity.setAnimation(resId)
        } else {
            // Handle animation doesn't exists
        }
    }


    /**Activity onStart/Stop**/
    override fun onStart() {
        super.onStart()
        Intent(this, ActivityChronometerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    override fun onStop() {
        super.onStop()
    }


    /**Start of ActivityChronometerService**/
    private fun startChronometerService(){
        // Avvio il servizio se non è già in esecuzione
        val intent = Intent(this, ActivityChronometerService::class.java)
        ContextCompat.startForegroundService(this, intent)
        //startService(intent)
    }


    /**Permissions**/
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Verifica permesso per le notifiche (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Verifica permesso per il riconoscimento attività (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  // API 29 è Android Q
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
        }

        Log.d("Permissions", "Permessi da richiedere: $permissionsToRequest")

        // Richiedo i permessi se ce ne sono da richiedere
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, ActivityChronometerService::class.java).apply {
            this.action = action
        }
        // Utilizzo startForegroundService() per avviare il servizio in modo sicuro come
        // un servizio in primo piano (da Android Oreo)
        ContextCompat.startForegroundService(this, intent)
    }

}
