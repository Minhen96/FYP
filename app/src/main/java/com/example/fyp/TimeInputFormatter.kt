package com.example.fyp

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.Calendar

class TimeInputFormatter(private val editText: EditText) : TextWatcher {
    private var current = ""
    private val ddmmyyyy = "HHMM"
    private val cal = Calendar.getInstance()

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.toString() != current) {
            var clean = s.toString().replace("[^\\d.]".toRegex(), "")
            val cleanC = current.replace("[^\\d.]".toRegex(), "")

            val cl = clean.length
            var sel = cl
            var i = 2
            while (i <= cl && i < 6) {
                sel++
                i += 2
            }
            if (clean == cleanC) sel--

            if (clean.length < 4) {
                clean += ddmmyyyy.substring(clean.length)
            } else {
                var hour = Integer.parseInt(clean.substring(0, 2))
                var min = Integer.parseInt(clean.substring(2, 4))

                if (hour > 23) hour = 23
                if (min > 59) min = 59

                clean = String.format("%02d%02d", hour, min)
            }

            clean = String.format("%s:%s", clean.substring(0, 2),
                clean.substring(2, 4))

            sel = if (sel < 0) 0 else sel
            current = clean
            editText.setText(current)
            editText.setSelection(if (sel < current.length) sel else current.length)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun afterTextChanged(s: Editable) {}
}