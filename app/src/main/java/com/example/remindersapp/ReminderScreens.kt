package com.example.remindersapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Recordatorios", fontWeight = FontWeight.Bold) },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.loadReminders() },
                    modifier = Modifier.weight(1f)
                ) { Text("Recargar", fontSize = 12.sp) }

                OutlinedButton(
                    onClick = { viewModel.simulateEmptyState() },
                    modifier = Modifier.weight(1f)
                ) { Text("Vacío", fontSize = 12.sp) }

                OutlinedButton(
                    onClick = { viewModel.simulateErrorState() },
                    modifier = Modifier.weight(1f)
                ) { Text("Error", fontSize = 12.sp) }
            }

            when (val state = uiState) {
                is ListUiState.Loading -> LoadingView("Cargando recordatorios...")
                is ListUiState.Empty -> EmptyOrErrorView(
                    message = "No hay recordatorios registrados aún.",
                    buttonText = "Crear el primero",
                    onActionClick = onCrearRecordatorio
                )
                is ListUiState.Error -> EmptyOrErrorView(
                    message = state.message,
                    buttonText = "Reintentar",
                    onActionClick = { viewModel.loadReminders() }
                )
                is ListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
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
                    if (isTitleInvalid) {
                        Text("El título debe tener al menos 4 caracteres.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("Descripción (Opcional)") },
                isError = isDescInvalid,
                supportingText = {
                    Text("${uiState.description.length} / 200 caracteres")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Categoría *", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InMemoryReminderRepository.categories.forEach { category ->
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
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = { viewModel.saveNewReminder(onSuccessNavigation = onGuardarSuccess) },
                    enabled = viewModel.isFormValid() && !uiState.isSaving,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar")
                    }
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
                title = { Text("Detalle del Recordatorio") },
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
                is DetailUiState.Loading -> LoadingView("Obteniendo detalle...")
                is DetailUiState.Error -> EmptyOrErrorView(
                    message = state.message,
                    buttonText = "Volver al listado",
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
                            Box(modifier = Modifier
                                .background(categoryColor, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = reminder.category,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()
                            Text(
                                text = "Descripción:",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Text(
                                text = reminder.description ?: "Sin descripción adicional.",
                                style = MaterialTheme.typography.bodyLarge
                            )
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
                        }
                    }
                }
            }
        }
    }
}