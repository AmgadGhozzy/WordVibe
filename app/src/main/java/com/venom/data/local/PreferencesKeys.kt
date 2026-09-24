package com.venom.data.local

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferencesKeys {
    val UNLOCKED_LEVELS: Preferences.Key<Set<String>> = stringSetPreferencesKey("unlocked_levels")
    val PLACEMENT_COMPLETED: Preferences.Key<Boolean> = booleanPreferencesKey("placement_completed")
    val ASSIGNED_UNIT: Preferences.Key<Int> = intPreferencesKey("assigned_unit")
    val SEED_COMPLETED: Preferences.Key<Boolean> = booleanPreferencesKey("corpus_seed_completed")
}