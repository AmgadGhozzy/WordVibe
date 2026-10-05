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
    val ONBOARDING_DONE: Preferences.Key<Boolean> = booleanPreferencesKey("onboarding_done")

    // Spelling hint budget (ADR-0009)
    val HINTS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("spelling_hints_enabled")
    val MAX_HINTS_PER_WORD: Preferences.Key<Int> = intPreferencesKey("spelling_max_hints")
    val HINT_CREDIT_POLICY: Preferences.Key<String> =
        androidx.datastore.preferences.core.stringPreferencesKey("spelling_hint_credit_policy")
    val HINT_CREDIT_LIMIT: Preferences.Key<Int> = intPreferencesKey("spelling_hint_credit_limit")
}