package com.example.remindersapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository: ReminderRepository = OfflineFirstReminderRepository(database.reminderDao())
    private val prefsManager = UserPreferencesManager(application)

    // Preferencias persistentes (DataStore)
    val userPreferences: StateFlow<UserPreferences> = prefsManager.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    // Estado de sincronización / mensajes de red
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Combina la fuente única de verdad (Room) con las preferencias (DataStore)
    val listUiState: StateFlow<ListUiState> = combine(
        repository.remindersFlow,
        userPreferences
    ) { reminders, prefs ->
        val filtered = if (prefs.showCompleted) {
            reminders
        } else {
            reminders.filter { it.status is ReminderStatus.Pending }
        }

        val sorted = when (prefs.sortOrder) {
            SortOrder.BY_ID -> filtered.sortedByDescending { it.id }
            SortOrder.BY_PRIORITY -> filtered.sortedBy { it.priority.ordinal }
            SortOrder.BY_TITLE -> filtered.sortedBy { it.title.lowercase() }
        }

        if (sorted.isEmpty()) ListUiState.Empty else ListUiState.Success(sorted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListUiState.Loading
    )

    // Estado del formulario de creación
    private val _createUiState = MutableStateFlow(CreateUiState())
    val createUiState: StateFlow<CreateUiState> = _createUiState.asStateFlow()

    // Estado del detalle
    private val _detailUiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val detailUiState: StateFlow<DetailUiState> = _detailUiState.asStateFlow()

    init {
        syncNow()
    }

    // --- SINCRONIZACIÓN REMOTA ---
    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            val result = repository.syncWithRemote()
            _isSyncing.value = false
            result.onSuccess { msg -> _syncMessage.value = msg }
                .onFailure { err -> _syncMessage.value = err.message }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // --- PREFERENCIAS (DATASTORE) ---
    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch { prefsManager.updateDarkTheme(isDark) }
    }

    fun toggleShowCompleted(show: Boolean) {
        viewModelScope.launch { prefsManager.updateShowCompleted(show) }
    }

    fun changeSortOrder(order: SortOrder) {
        viewModelScope.launch { prefsManager.updateSortOrder(order) }
    }

    // --- ACCIONES CRUD EN ROOM ---
    fun toggleReminderCompleted(id: Int) {
        viewModelScope.launch { repository.toggleCompleted(id) }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch { repository.deleteReminder(id) }
    }

    // --- FORMULARIO ---
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
        val current = _createUiState.value
        return current.title.trim().length >= 4 &&
                current.description.length <= 200 &&
                OfflineFirstReminderRepository.categories.contains(current.category) &&
                current.priority != null
    }

    fun saveNewReminder(onSuccessNavigation: () -> Unit) {
        if (!isFormValid()) return

        viewModelScope.launch {
            _createUiState.update { it.copy(isSaving = true) }
            val current = _createUiState.value
            val newReminder = Reminder(
                id = 0,
                title = current.title.trim(),
                category = current.category,
                priority = current.priority!!,
                status = ReminderStatus.Pending,
                description = current.description.trim().ifEmpty { null }
            )
            repository.saveReminder(newReminder)
            _createUiState.value = CreateUiState()
            onSuccessNavigation()
        }
    }

    fun resetForm() {
        _createUiState.value = CreateUiState()
    }

    // --- DETALLE ---
    fun loadReminderDetail(id: Int) {
        viewModelScope.launch {
            _detailUiState.value = DetailUiState.Loading
            val reminder = repository.getReminderById(id)
            if (reminder != null) {
                _detailUiState.value = DetailUiState.Success(reminder)
            } else {
                _detailUiState.value = DetailUiState.Error("No se encontró el recordatorio en la base local.")
            }
        }
    }
}
