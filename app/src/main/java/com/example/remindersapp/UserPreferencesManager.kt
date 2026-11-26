package com.example.remindersapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class SortOrder { BY_ID, BY_PRIORITY, BY_TITLE }

data class UserPreferences(
    val isDarkTheme: Boolean = false,
    val showCompleted: Boolean = true,
    val sortOrder: SortOrder = SortOrder.BY_ID
)

class UserPreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isDark = preferences[PreferencesKeys.DARK_THEME] ?: false
            val showComp = preferences[PreferencesKeys.SHOW_COMPLETED] ?: true
            val sortStr = preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_ID.name

            val validSort = try {
                SortOrder.valueOf(sortStr)
            } catch (e: IllegalArgumentException) {
                SortOrder.BY_ID
            }

            UserPreferences(
                isDarkTheme = isDark,
                showCompleted = showComp,
                sortOrder = validSort
            )
        }

    suspend fun updateDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DARK_THEME] = isDark }
    }

    suspend fun updateShowCompleted(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_COMPLETED] = show }
    }

    suspend fun updateSortOrder(order: SortOrder) {
        context.dataStore.edit { it[PreferencesKeys.SORT_ORDER] = order.name }
    }
}
