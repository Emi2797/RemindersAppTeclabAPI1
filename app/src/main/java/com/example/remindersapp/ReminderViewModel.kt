package com.example.remindersapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val repository: ReminderRepository = InMemoryReminderRepository
) : ViewModel() {

    private val _listUiState = MutableStateFlow<ListUiState>(ListUiState.Loading)
    val listUiState: StateFlow<ListUiState> = _listUiState.asStateFlow()

    private val _createUiState = MutableStateFlow(CreateUiState())
    val createUiState: StateFlow<CreateUiState> = _createUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val detailUiState: StateFlow<DetailUiState> = _detailUiState.asStateFlow()

    init {
        loadReminders()
    }

    fun loadReminders() {
        viewModelScope.launch {
            _listUiState.value = ListUiState.Loading
            delay(1500)
            try {
                val list = repository.getReminders()
                if (list.isEmpty()) {
                    _listUiState.value = ListUiState.Empty
                } else {
                    _listUiState.value = ListUiState.Success(list)
                }
            } catch (e: Exception) {
                _listUiState.value = ListUiState.Error("No se pudieron cargar los recordatorios. Intente nuevamente.")
            }
        }
    }

    fun simulateEmptyState() {
        _listUiState.value = ListUiState.Empty
    }

    fun simulateErrorState() {
        _listUiState.value = ListUiState.Error("Error de conexión simulado con opción de reintento.")
    }

    fun onTitleChanged(newTitle: String) {
        _createUiState.update { it.copy(title = newTitle, titleError = null) }
    }

    fun onDescriptionChanged(newDesc: String) {
        _createUiState.update { it.copy(description = newDesc, descriptionError = null) }
    }

    fun onCategoryChanged(newCat: String) {
        _createUiState.update { it.copy(category = newCat) }
    }

    fun onPriorityChanged(newPriority: Priority) {
        _createUiState.update { it.copy(priority = newPriority) }
    }

    fun isFormValid(): Boolean {
        val currentState = _createUiState.value
        val cleanTitle = currentState.title.trim()

        val isTitleValid = cleanTitle.length >= 4
        val isDescriptionValid = currentState.description.length <= 200
        val isCategoryValid = currentState.category.isNotEmpty() &&
                InMemoryReminderRepository.categories.contains(currentState.category)
        val isPriorityValid = currentState.priority != null

        return isTitleValid && isDescriptionValid && isCategoryValid && isPriorityValid
    }

    fun saveNewReminder(onSuccessNavigation: () -> Unit) {
        if (!isFormValid()) return

        viewModelScope.launch {
            _createUiState.update { it.copy(isSaving = true, generalError = null) }
            delay(1500)

            try {
                val currentState = _createUiState.value
                val newId = (repository.getReminders().maxOfOrNull { it.id } ?: 0) + 1

                val newReminder = Reminder(
                    id = newId,
                    title = currentState.title.trim(),
                    category = currentState.category,
                    priority = currentState.priority!!,
                    status = ReminderStatus.Pending,
                    description = currentState.description.trim().ifEmpty { null }
                )

                repository.saveReminder(newReminder)
                _createUiState.update { it.copy(isSaving = false, isSuccess = true) }
                loadReminders()
                onSuccessNavigation()
                _createUiState.update { CreateUiState() }

            } catch (e: Exception) {
                _createUiState.update { it.copy(isSaving = false, generalError = "Error al intentar guardar el registro.") }
            }
        }
    }

    fun resetForm() {
        _createUiState.value = CreateUiState()
    }

    fun loadReminderDetail(id: Int) {
        viewModelScope.launch {
            _detailUiState.value = DetailUiState.Loading
            delay(1000)
            val reminder = repository.getReminderById(id)
            if (reminder != null) {
                _detailUiState.value = DetailUiState.Success(reminder)
            } else {
                _detailUiState.value = DetailUiState.Error("No se encontró el registro con el identificador proporcionado.")
            }
        }
    }
}