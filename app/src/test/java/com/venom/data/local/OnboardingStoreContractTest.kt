package com.venom.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Ticket 06: onboarding is asked exactly once and remembered across restarts.
 */
class OnboardingStoreContractTest {

    @Test
    fun `fresh install has onboarding incomplete`() = runBlocking {
        val file = tempPrefsFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = OnboardingStore(PreferenceDataStoreFactory.create(scope = scope) { file })
            assertFalse(store.onboardingDone.first())
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test
    fun `completing onboarding persists across store instances`() = runBlocking {
        val file = tempPrefsFile()
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val first = OnboardingStore(PreferenceDataStoreFactory.create(scope = firstScope) { file })
            first.complete()
            assertTrue(first.onboardingDone.first())
        } finally {
            firstScope.cancel()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val second = OnboardingStore(PreferenceDataStoreFactory.create(scope = secondScope) { file })
            assertTrue(
                second.onboardingDone.first(),
                "onboarding must survive a process restart (asked exactly once)",
            )
        } finally {
            secondScope.cancel()
            file.delete()
        }
    }

    private fun tempPrefsFile(): File =
        File.createTempFile("onboarding_store", ".preferences_pb").apply { delete() }
}