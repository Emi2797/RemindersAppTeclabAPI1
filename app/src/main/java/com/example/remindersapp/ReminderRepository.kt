package com.example.remindersapp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.SocketTimeoutException

// --- FUNCIONES DE MAPEO EXPLÍCITAS (Consigna 3) ---
fun ReminderEntity.toDomain(): Reminder {
    return Reminder(
        id = this.id,
        title = this.title,
        category = this.category,
        priority = try { Priority.valueOf(this.priority) } catch (e: Exception) { Priority.MEDIUM },
        status = if (this.isCompleted) ReminderStatus.Completed else ReminderStatus.Pending,
        description = this.description
    )
}

fun ReminderDto.toEntity(): ReminderEntity {
    return ReminderEntity(
        remoteId = this.id,
        title = this.title,
        description = "Sincronizado desde servidor remoto",
        category = "Trabajo",
        priority = Priority.MEDIUM.name,
        isCompleted = this.completed,
        syncStatus = SyncStatus.SYNCED.name
    )
}

// --- INTERFAZ Y REPOSITORIO LOCAL-FIRST ---
interface ReminderRepository {
    val remindersFlow: Flow<List<Reminder>>
    suspend fun getReminderById(id: Int): Reminder?
    suspend fun saveReminder(reminder: Reminder)
    suspend fun toggleCompleted(id: Int)
    suspend fun deleteReminder(id: Int)
    suspend fun syncWithRemote(): Result<String>
}

class OfflineFirstReminderRepository(
    private val dao: ReminderDao,
    private val api: ReminderApiService = RetrofitClient.apiService
) : ReminderRepository {

    companion object {
        val categories = listOf("Trabajo", "Personal", "Estudios", "Salud")
    }

    // Mutex evita que dos eventos disparen sincronización simultánea y dupliquen registros
    private val syncMutex = Mutex()

    // Room es la ÚNICA fuente de verdad para la interfaz (Consigna 2 y 3)
    override val remindersFlow: Flow<List<Reminder>> = dao.getAllRemindersFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getReminderById(id: Int): Reminder? {
        return dao.getReminderById(id)?.toDomain()
    }

    override suspend fun saveReminder(reminder: Reminder) {
        // 1. Se guarda primero localmente como pendiente de sincronización (Local-First)
        val entity = ReminderEntity(
            title = reminder.title,
            description = reminder.description,
            category = reminder.category,
            priority = reminder.priority.name,
            isCompleted = reminder.status is ReminderStatus.Completed,
            syncStatus = SyncStatus.PENDING_CREATE.name
        )
        dao.insertReminder(entity)

        // 2. Intenta sincronizar en segundo plano si hay red
        syncWithRemote()
    }

    override suspend fun toggleCompleted(id: Int) {
        val current = dao.getReminderById(id) ?: return
        val updated = current.copy(
            isCompleted = !current.isCompleted,
            syncStatus = if (current.remoteId != null) SyncStatus.PENDING_UPDATE.name else SyncStatus.PENDING_CREATE.name
        )
        dao.updateReminder(updated)
        syncWithRemote()
    }

    override suspend fun deleteReminder(id: Int) {
        val current = dao.getReminderById(id) ?: return
        if (current.remoteId == null) {
            // Si nunca llegó al servidor, se borra directo de Room
            dao.deleteReminder(current)
        } else {
            // Se marca para borrado remoto
            dao.updateReminder(current.copy(syncStatus = SyncStatus.PENDING_DELETE.name))
            syncWithRemote()
        }
    }

    override suspend fun syncWithRemote(): Result<String> = syncMutex.withLock {
        return try {
            // 1. Subir cambios locales pendientes (Push)
            val unsynced = dao.getUnsyncedReminders()
            for (item in unsynced) {
                when (item.syncStatus) {
                    SyncStatus.PENDING_CREATE.name -> {
                        val response = api.createRemoteReminder(
                            ReminderDto(id = null, title = item.title, completed = item.isCompleted)
                        )
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id ?: (1000 + item.id)
                            dao.updateReminder(item.copy(remoteId = serverId, syncStatus = SyncStatus.SYNCED.name))
                        }
                    }
                    SyncStatus.PENDING_UPDATE.name -> {
                        val rId = item.remoteId ?: continue
                        val response = api.updateRemoteReminder(
                            rId, ReminderDto(id = rId, title = item.title, completed = item.isCompleted)
                        )
                        if (response.isSuccessful) {
                            dao.updateReminder(item.copy(syncStatus = SyncStatus.SYNCED.name))
                        }
                    }
                    SyncStatus.PENDING_DELETE.name -> {
                        dao.deleteReminder(item)
                    }
                }
            }

            // 2. Descargar novedades del servidor sin duplicar (Pull)
            val remoteResponse = api.fetchRemoteReminders()
            if (remoteResponse.isSuccessful) {
                val remoteList = remoteResponse.body() ?: emptyList()
                for (dto in remoteList) {
                    val rId = dto.id ?: continue
                    val existing = dao.getReminderByRemoteId(rId)
                    if (existing == null) {
                        dao.insertReminder(dto.toEntity())
                    }
                }
                Result.success("Sincronización completada correctamente")
            } else {
                // Manejo controlado de códigos HTTP (Consigna 4)
                val errorMsg = when (remoteResponse.code()) {
                    400, 404 -> "Solicitud inválida o recurso no encontrado (${remoteResponse.code()})"
                    in 500..599 -> "Error interno del servidor remoto (${remoteResponse.code()})"
                    else -> "Error HTTP inesperado: ${remoteResponse.code()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Tiempo de espera agotado (Timeout). Trabajando en modo offline."))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet. Los cambios quedaron guardados localmente."))
        } catch (e: Exception) {
            Result.failure(Exception("Error inesperado al sincronizar: ${e.localizedMessage}"))
        }
    }
}
