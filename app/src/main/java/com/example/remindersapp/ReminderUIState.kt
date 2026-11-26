package com.example.remindersapp

// Estados de la pantalla de listado (Consigna 3 - Exhaustivos y mutuamente excluyentes)
sealed interface ListUiState {
    object Loading : ListUiState
    object Empty : ListUiState
    data class Success(val reminders: List<Reminder>) : ListUiState
    data class Error(val message: String) : ListUiState
}

// Estados y validaciones de la pantalla de creación (Consigna 3)
data class CreateUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: Priority? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val generalError: String? = null,
    // Errores específicos de validación para los componentes controlados
    val titleError: String? = null,
    val descriptionError: String? = null
)

// Estados de la pantalla de detalle (Consigna 3)
sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val reminder: Reminder) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
