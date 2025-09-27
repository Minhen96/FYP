package com.example.fyp

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.widget.DatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomDatePickerDialog(
    context: Context,
    listener: OnDateSetListener,
    year: Int,
    month: Int,
    day: Int,
    private val unavailableDates: List<String>,
    private val unavailableDays: List<String>
) : DatePickerDialog(context, listener, year, month, day) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val datePicker = findViewById<DatePicker>(Resources.getSystem().getIdentifier("datePicker", "id", "android"))

        datePicker?.let {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayNames = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

            it.setOnDateChangedListener { view, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateString = dateFormat.format(calendar.time)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayName = dayNames[dayOfWeek]

                val isUnavailableDate = unavailableDates.any { range ->
                    val (start, end) = range.split(" to ")
                    dateString in start..end
                }

                val isUnavailableDay = unavailableDays.contains(dayName)

                if (isUnavailableDate || isUnavailableDay) {
                    // Disable the date
                    view.setEnabled(false)
                } else {
                    view.setEnabled(true)
                }
            }
        }
    }
}