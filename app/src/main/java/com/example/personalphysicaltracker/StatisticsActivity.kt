package com.example.personalphysicaltracker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.personalphysicaltracker.data.model.ActivityType
import com.example.personalphysicaltracker.data.repository.ActivityRepository
import com.example.personalphysicaltracker.utils.DateUtils

// Librerie grafici
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.color.MaterialColors

import java.time.Instant
import java.time.ZoneId

/**
 * StatisticsActivity is responsible for displaying statistics about user activities.
 * Particularly:
 * - A pie chart to show the percentage of different activity types over the month.
 * - A bar chart to display daily steps count per week.
 *
 * Data is retrieved from the ActivityRepository and visualized using MPAndroidChart.
 */

class StatisticsActivity : AppCompatActivity() {

    private lateinit var pieChartActivityTypes: PieChart
    private lateinit var activityRepository: ActivityRepository
    private lateinit var barChartDailySteps: com.github.mikephil.charting.charts.BarChart
    private lateinit var btnPreviousWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)

        // Applicati margini adeguati per adattarsi alle barre di sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.txtStatisticsTitle)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inizializzazione componenti UI
        pieChartActivityTypes = findViewById(R.id.pieChartActivityTypes)
        barChartDailySteps = findViewById(R.id.barChartDailySteps)
        btnPreviousWeek = findViewById(R.id.btnPreviousWeek)
        btnNextWeek = findViewById(R.id.btnNextWeek)

        // Repository
        activityRepository = ActivityRepository(application)

        // Setup dati e grafici
        setupPieChart()
        calculateMonthlyPercentages()
        setupTitle()
        setupBarChart()
        setupWeekNavigation()
    }


    private fun setupTitle() {
        val chartTitle = findViewById<TextView>(R.id.txtChartTitle)
        chartTitle.text = "Monthly Activity Summary"
    }

    private fun setupPieChart() {
        val activityTypes = ActivityType.entries
        val startOfMonth = DateUtils.getStartOfMonthInMillis()
        val endOfMonth = DateUtils.getEndOfMonthInMillis()
        val entries = mutableListOf<PieEntry>()

        activityTypes.forEach { activityType ->
            activityRepository.getTotalDurationForTypeAndPeriod(activityType.name, startOfMonth, endOfMonth)
                .observe(this) { totalDuration ->
                    if (totalDuration != null && totalDuration > 0) {
                        // Aggiungo l'entry, con il tipo di attività e la durata, al grafico a torta
                        entries.add(PieEntry(totalDuration.toFloat(), activityType.name))

                        val dataSet = PieDataSet(entries, "Activity Durations (Monthly)")
                        dataSet.setColors(*ColorTemplate.COLORFUL_COLORS)
                        dataSet.setDrawValues(false) // Disabilita le etichette sui valori

                        val pieData = PieData(dataSet)
                        pieData.setValueTextSize(12f)
                        pieData.setValueFormatter(object : ValueFormatter() {
                            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                                return pieEntry?.label ?: "" // Mostra solo il tipo di attività
                            }
                        })

                        pieChartActivityTypes.data = pieData
                        configureLegend(pieChartActivityTypes.legend, entries) // Configura la legenda del grafico
                        pieChartActivityTypes.setHoleRadius(40f) // Riduci il centro bianco
                        pieChartActivityTypes.setTransparentCircleRadius(45f)
                        pieChartActivityTypes.description.isEnabled = false
                        pieChartActivityTypes.invalidate() // Aggiorna il grafico
                    }
                }
        }
    }

    // Configuro la legenda del grafico
    private fun configureLegend(legend: Legend, entries: List<PieEntry>) {
        legend.isEnabled = true
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        // Adatta il colore della legenda alla modalità scura/chiara
        legend.textColor = MaterialColors.getColor(this, android.R.attr.textColorPrimary, Color.BLACK)
        legend.textSize = 12f

        // Legenda personalizzata con le durate formattate
        legend.setCustom(entries.mapIndexed { index, entry ->
            val formattedTime = DateUtils.formatMillisToHHMM(entry.value.toLong())
            LegendEntry(
                "${entry.label} (${formattedTime})", // Testo personalizzato
                Legend.LegendForm.CIRCLE,           // Forma del simbolo
                10f,                                // Dimensione del simbolo
                2f,                                 // Spaziatura
                null,                               // LineStyle
                ColorTemplate.COLORFUL_COLORS[index % ColorTemplate.COLORFUL_COLORS.size] // Colore
            )
        })
    }


    private fun calculateMonthlyPercentages() {
        val startOfMonth = DateUtils.getStartOfMonthInMillis()
        val endOfMonth = DateUtils.getEndOfMonthInMillis()
        Log.d("StatisticsActivity", "StartOfMonth: ${Instant.ofEpochMilli(startOfMonth).atZone(ZoneId.systemDefault()).toLocalDate()}")
        Log.d("StatisticsActivity", "EndOfMonth: ${Instant.ofEpochMilli(endOfMonth).atZone(ZoneId.systemDefault()).toLocalDate()}")
        Log.d("StatisticsActivity", "Start of Month: $startOfMonth, End of Month: $endOfMonth")
        val totalMillisInMonth = DateUtils.getTotalMillisInMonth()

        // Ottiengo il totale registrato dal database
        activityRepository.getMonthlySummary(startOfMonth, endOfMonth).observe(this) { registeredDuration ->
            Log.d("StatisticsActivity", "Durata totale registrata: $registeredDuration")
            val registeredMillis = registeredDuration ?: 0L
            val unknownMillis = totalMillisInMonth - registeredMillis

            // Calcolo le percentuali
            val registeredPercentage = (registeredMillis.toDouble() / totalMillisInMonth * 100).toInt()
            val unknownPercentage = (unknownMillis.toDouble() / totalMillisInMonth * 100).toInt()

            // Mostro la scritta sotto il grafico
            findViewById<TextView>(R.id.txtMonthlySummary).text =
                "You registered the $registeredPercentage% of the month, the remaining $unknownPercentage% is Unknown."
        }
    }


    private fun setupBarChart() {
        val textColor = MaterialColors.getColor(this, android.R.attr.textColorPrimary, Color.BLACK)

        barChartDailySteps.apply {
            description.isEnabled = false
            setFitBars(true)
            setDrawGridBackground(false)
            axisLeft.axisMinimum = 0f
            axisLeft.granularity = 1f
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.textColor = textColor
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.textColor = textColor
        }
        loadWeeklyData(DateUtils.getStartOfWeekInMillis(), DateUtils.getEndOfWeekInMillis())

    }


    private fun loadWeeklyData(startOfWeek: Long, endOfWeek: Long) {
        val textColor = MaterialColors.getColor(this, android.R.attr.textColorPrimary, Color.BLACK)
        // Inizializza i giorni della settimana con valori di default
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dailyStepsMap = daysOfWeek.associateWith { 0f }.toMutableMap() // Default 0 passi

        // Ottiengo i dati reali dal database
        activityRepository.getStepsForWeek(startOfWeek, endOfWeek).observe(this) { dailySteps ->
            // Sovrascrivo i valori di default con i dati reali
            dailySteps.forEachIndexed { index, steps ->
                dailyStepsMap[daysOfWeek[index]] = steps.toFloat()
            }

            // Creo le entries del grafico
            val entries = dailyStepsMap.values.mapIndexed { index, steps ->
                BarEntry(index.toFloat(), steps)
            }

            val dataSet = BarDataSet(entries, "Steps")
            dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = textColor

            val barData = BarData(dataSet)
            barData.barWidth = 0.8f

            barChartDailySteps.data = barData
            barChartDailySteps.invalidate()

            // Formatter per visualizzare sempre i giorni
            barChartDailySteps.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val dayIndex = (value.toInt() % 7)
                    return daysOfWeek.getOrElse(dayIndex) { "" }
                }
            }
        }
    }

    private var currentWeekOffset = 0

    private fun setupWeekNavigation() {
        val txtWeekRange = findViewById<TextView>(R.id.txtWeekRange)
        updateWeekRangeText(txtWeekRange)

        btnPreviousWeek.setOnClickListener {
            currentWeekOffset--
            updateBarChartForCurrentWeek()
            updateWeekRangeText(txtWeekRange)
        }

        btnNextWeek.setOnClickListener {
            currentWeekOffset++
            updateBarChartForCurrentWeek()
            updateWeekRangeText(txtWeekRange)
        }
    }


    private fun updateWeekRangeText(txtWeekRange: TextView) {
        val startOfWeek = DateUtils.getStartOfWeekInMillis(currentWeekOffset)
        val endOfWeek = DateUtils.getEndOfWeekInMillis(currentWeekOffset)
        txtWeekRange.text = DateUtils.formatDateRange(startOfWeek, endOfWeek)
    }


    private fun updateBarChartForCurrentWeek() {
        val startOfWeek = DateUtils.getStartOfWeekInMillis(currentWeekOffset)
        val endOfWeek = DateUtils.getEndOfWeekInMillis(currentWeekOffset)
        loadWeeklyData(startOfWeek, endOfWeek)
    }

}