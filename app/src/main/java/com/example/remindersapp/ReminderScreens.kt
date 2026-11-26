package com.example.remindersapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderViewModel,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Int) -> Unit
) {
    val uiState by viewModel.listUiState.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recordatorios Local-First", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    TextButton(onClick = { viewModel.syncNow() }, enabled = !isSyncing) {
                        Text(if (isSyncing) "Sinc..." else "↻ Sync", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Panel de Preferencias Persistentes (DataStore - Consigna 1)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Oscuro:", fontSize = 12.sp)
                            Switch(
                                checked = prefs.isDarkTheme,
                                onCheckedChange = { viewModel.toggleDarkTheme(it) },
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Completados:", fontSize = 12.sp)
                            Switch(
                                checked = prefs.showCompleted,
                                onCheckedChange = { viewModel.toggleShowCompleted(it) },
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Orden:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        SortOrder.entries.forEach { order ->
                            FilterChip(
                                selected = prefs.sortOrder == order,
                                onClick = { viewModel.changeSortOrder(order) },
                                label = {
                                    Text(
                                        when (order) {
                                            SortOrder.BY_ID -> "Reciente"
                                            SortOrder.BY_PRIORITY -> "Prioridad"
                                            SortOrder.BY_TITLE -> "Título"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Aviso de estado de red / sincronización (Consigna 4)
            syncMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearSyncMessage() }) {
                            Text("OK", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Listado desde Room (Única fuente de verdad - Consigna 2)
            when (val state = uiState) {
                is ListUiState.Loading -> LoadingView("Cargando base local...")
                is ListUiState.Empty -> EmptyOrErrorView(
                    message = "No hay recordatorios visibles con los filtros actuales.",
                    buttonText = "Crear recordatorio",
                    onActionClick = onCrearRecordatorio
                )
                is ListUiState.Error -> EmptyOrErrorView(
                    message = state.message,
                    buttonText = "Reintentar",
                    onActionClick = { viewModel.syncNow() }
                )
                is ListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        items(state.reminders, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onClick = { onRecordatorioClick(reminder.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCreateScreen(
    viewModel: ReminderViewModel,
    onGuardarSuccess: () -> Unit,
    onCancelar: () -> Unit
) {
    val uiState by viewModel.createUiState.collectAsState()
    val isTitleInvalid = uiState.title.isNotEmpty() && uiState.title.trim().length < 4
    val isDescInvalid = uiState.description.length > 200

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recordatorio") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetForm()
                        onCancelar()
                    }) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Título *") },
                isError = isTitleInvalid,
                supportingText = {
                    if (isTitleInvalid) Text("El título debe tener al menos 4 caracteres.")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("Descripción (Opcional)") },
                isError = isDescInvalid,
                supportingText = { Text("${uiState.description.length} / 200 caracteres") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Categoría *", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OfflineFirstReminderRepository.categories.forEach { category ->
                    FilterChip(
                        selected = uiState.category == category,
                        onClick = { viewModel.onCategoryChanged(category) },
                        label = { Text(category, fontSize = 12.sp) }
                    )
                }
            }

            Text("Prioridad *", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { priority ->
                    FilterChip(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.onPriorityChanged(priority) },
                        label = { Text(priority.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.resetForm()
                        onCancelar()
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Cancelar") }
                Button(
                    onClick = { viewModel.saveNewReminder(onSuccessNavigation = onGuardarSuccess) },
                    enabled = viewModel.isFormValid() && !uiState.isSaving,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar")
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminderId: Int,
    viewModel: ReminderViewModel,
    onVolver: () -> Unit
) {
    LaunchedEffect(reminderId) {
        viewModel.loadReminderDetail(reminderId)
    }
    val uiState by viewModel.detailUiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle (Room)") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> LoadingView("Consultando Room...")
                is DetailUiState.Error -> EmptyOrErrorView(
                    message = state.message,
                    buttonText = "Volver",
                    onActionClick = onVolver
                )
                is DetailUiState.Success -> {
                    val reminder = state.reminder
                    val categoryColor = categoryColors[reminder.category] ?: Color.Gray
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(categoryColor, shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(reminder.category, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()
                            Text("Descripción:", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Text(reminder.description ?: "Sin descripción adicional.")
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Prioridad: ${reminder.priority.name}", fontWeight = FontWeight.Bold)
                                val statusText = when (reminder.status) {
                                    is ReminderStatus.Pending -> "Pendiente ⏳"
                                    is ReminderStatus.Completed -> "Completado ✅"
                                }
                                Text(statusText, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
// Botones CRUD directos sobre Room (Consigna 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.toggleReminderCompleted(reminder.id)
                                        onVolver()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (reminder.status is ReminderStatus.Completed) "Marcar Pendiente" else "Completar ✅")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteReminder(reminder.id)
                                        onVolver()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Eliminar 🗑️", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}