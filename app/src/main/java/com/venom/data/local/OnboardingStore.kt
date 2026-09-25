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

    suspend fun complete() {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_DONE] = true }
    }
}