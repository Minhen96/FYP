package com.example.fyp

import java.util.Date


data class Message(
    val sender: String = "",
    val content: String = "",
    val type: String = "",
    val timestamp: Date = Date(),
    val read: Boolean = false
)