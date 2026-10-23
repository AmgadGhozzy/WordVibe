package com.venom.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Ticket 06: onboarding state. Asked exactly once and remembered across restarts.
 */
class OnboardingStore(
    private val dataStore: DataStore<Preferences>,
) {
    val onboardingDone: Flow<Boolean> =
        dataStore.data.map { it[PreferencesKeys.ONBOARDING_DONE] ?: false }

    val appTheme: Flow<com.venom.domain.model.AppTheme> =
        dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.APP_THEME]
            com.venom.domain.model.AppTheme.entries.find { it.name == raw }
                ?: com.venom.domain.model.AppTheme.SYSTEM
        }

    suspend fun complete() {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_DONE] = true }
    }
}