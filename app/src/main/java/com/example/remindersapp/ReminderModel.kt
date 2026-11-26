package com.example.remindersapp

import androidx.compose.ui.graphics.Color


enum class Priority {
    LOW, MEDIUM, HIGH
}

sealed class ReminderStatus {
    object Pending : ReminderStatus()
    object Completed : ReminderStatus()
}

data class Reminder(
    val id: Int,
    val title: String,
    val category: String,
    val priority: Priority,
    val status: ReminderStatus,
    val description: String? = null
)

val categoryColors: Map<String, Color> = mapOf(
    "Trabajo" to Color(0xFF3F51B5),   // Azul
    "Personal" to Color(0xFF4CAF50),  // Verde
    "Estudios" to Color(0xFFFF9800),  // Naranja
    "Salud" to Color(0xFFE91E63)      // Rosa
)
