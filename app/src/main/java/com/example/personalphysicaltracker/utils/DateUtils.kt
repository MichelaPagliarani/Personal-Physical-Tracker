package com.example.personalphysicaltracker.utils

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

     fun getStartOfDayMillis(dateMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

     fun getEndOfDayMillis(dateMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }


     fun getStartOfWeekInMillis(offset: Int = 0): Long {
        val now = LocalDate.now().plusWeeks(offset.toLong())
        val firstDayOfWeek = now.with(ChronoField.DAY_OF_WEEK, 1) // Lunedì
        return firstDayOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

     fun getEndOfWeekInMillis(offset: Int = 0): Long {
        val now = LocalDate.now().plusWeeks(offset.toLong())
        val lastDayOfWeek = now.with(ChronoField.DAY_OF_WEEK, 7) // Domenica
        return lastDayOfWeek.atStartOfDay(ZoneId.systemDefault()).plusDays(1).toInstant().toEpochMilli()
    }


    fun getStartOfMonthInMillis(): Long {
        val now = LocalDate.now()
        val firstDayOfMonth = now.withDayOfMonth(1)
        return firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }


     fun getEndOfMonthInMillis(): Long {
        val now = LocalDate.now()
        val firstDayOfNextMonth = now.plusMonths(1).withDayOfMonth(1)
        return firstDayOfNextMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

     fun getStartOfWeekInMillis(): Long {
        val now = LocalDate.now()
        val firstDayOfWeek = now.with(ChronoField.DAY_OF_WEEK, 1) // Inizia da lunedì
        return firstDayOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

     fun getEndOfWeekInMillis(): Long {
        val now = LocalDate.now()
        val firstDayOfNextWeek = now.plusWeeks(1).with(ChronoField.DAY_OF_WEEK, 1)
        return firstDayOfNextWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

     fun getTotalMillisInMonth(): Long {
        val now = LocalDate.now()
        val firstDayOfMonth = now.withDayOfMonth(1)
        val lastDayOfMonth = now.plusMonths(1).withDayOfMonth(1).minusDays(1)

        val totalDays = ChronoUnit.DAYS.between(firstDayOfMonth, lastDayOfMonth) + 1
        return totalDays * 24 * 60 * 60 * 1000 // Totale in millisecondi
    }


     fun formatDateRange(startMillis: Long, endMillis: Long): String {
        val formatter = java.text.SimpleDateFormat("dd/MM", Locale.getDefault())
        val startDate = formatter.format(Date(startMillis))
        val endDate = formatter.format(Date(endMillis - 1)) // Fine giorno
        return "$startDate - $endDate"
    }

    // Funzione per formattare millisecondi in hh:mm
     fun formatMillisToHHMM(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return String.format("%02d:%02d", hours, minutes)
    }

}