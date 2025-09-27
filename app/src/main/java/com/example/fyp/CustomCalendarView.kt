package com.example.fyp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.CalendarView
import android.widget.DatePicker
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CalendarView(context, attrs, defStyleAttr) {

    private var unavailableDates: List<String> = emptyList()
    private var unavailableDays: List<String> = emptyList()
    private val grayPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayNames = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    fun setUnavailableDates(dates: List<String>) {
        unavailableDates = dates
        invalidate()
    }

    fun setUnavailableDays(days: List<String>) {
        unavailableDays = days
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val monthStartDate = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val monthEndDate = calendar.timeInMillis

        val cellWidth = width / 7f
        val cellHeight = height / 6f

        calendar.timeInMillis = monthStartDate
        var currentY = 0f

        while (calendar.timeInMillis <= monthEndDate) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val x = (dayOfWeek - 1) * cellWidth
            val dateString = dateFormat.format(calendar.time)
            val dayName = dayNames[dayOfWeek]

            if (isDateUnavailable(dateString) || isDateUnavailable(dayName)) {
                canvas.drawRect(x, currentY, x + cellWidth, currentY + cellHeight, grayPaint)
            }

            if (dayOfWeek == Calendar.SATURDAY) {
                currentY += cellHeight
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun isDateUnavailable(dateOrDay: String): Boolean {
        return unavailableDates.any { range ->
            val (start, end) = range.split(" to ")
            dateOrDay in start..end
        } || unavailableDays.contains(dateOrDay)
    }
}