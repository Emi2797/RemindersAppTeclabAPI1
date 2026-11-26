package com.example.remindersapp

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Estado de sincronización para estrategia Local-First (Consigna 3)
enum class SyncStatus { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE }

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: Int? = null, // Evita duplicados al sincronizar con el servidor
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val isCompleted: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED.name
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE syncStatus != 'PENDING_DELETE' ORDER BY id DESC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getReminderByRemoteId(remoteId: Int): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND syncStatus != 'PENDING_DELETE'")
    fun getPendingRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)
}

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reminders_local_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
