package com.example.remindersapp

interface ReminderRepository {
    suspend fun getReminders(): List<Reminder>
    suspend fun getReminderById(id: Int): Reminder?
    suspend fun saveReminder(reminder: Reminder)
}

object InMemoryReminderRepository : ReminderRepository {

    private val rawCategories = setOf("Trabajo", "Personal", "Estudios", "Salud")
    val categories: List<String> = rawCategories.toList()

    private val _reminders = mutableListOf(
        Reminder(
            1,
            "Entregar informe mensual",
            "Trabajo",
            Priority.HIGH,
            ReminderStatus.Pending,
            "Enviar al director por correo electrónico."
        ),
        Reminder(2, "Comprar mercadería", "Personal", Priority.LOW, ReminderStatus.Completed),
        Reminder(
            3,
            "Estudiar para examen Móviles",
            "Estudios",
            Priority.HIGH,
            ReminderStatus.Pending,
            "Repasar Jetpack Compose y Kotlin."
        ),
        Reminder(4, "Turno médico anual", "Salud", Priority.MEDIUM, ReminderStatus.Pending)
    )

    override suspend fun getReminders(): List<Reminder> {
        return _reminders.toList()
    }

    override suspend fun getReminderById(id: Int): Reminder? {
        return _reminders.find { it.id == id }
    }

    override suspend fun saveReminder(reminder: Reminder) {
        _reminders.add(reminder)
    }
}