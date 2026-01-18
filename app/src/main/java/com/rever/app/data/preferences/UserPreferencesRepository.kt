package com.rever.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rever_preferences")

class UserPreferencesRepository(private val context: Context) {
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val DAILY_REVISION_TIME = intPreferencesKey("daily_revision_time")
        private val PLAN_GENERATED = booleanPreferencesKey("plan_generated")
    }
    
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }
    
    val dailyRevisionTime: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DAILY_REVISION_TIME] ?: 60 // Default 60 minutes
        }
    
    val planGenerated: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PLAN_GENERATED] ?: false
        }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }
    
    suspend fun setDailyRevisionTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REVISION_TIME] = minutes
        }
    }
    
    suspend fun setPlanGenerated(generated: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PLAN_GENERATED] = generated
        }
    }
    
    suspend fun clearAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
