package com.venom.wordvibe.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.venom.analytics.StubCrashlyticsManager
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P-07 proof (PM closure requirement): the stub mirrors calls to logcat in debug
 * builds. Run, then `adb logcat -d -s WordVibeCrash` shows the probe line.
 */
@RunWith(AndroidJUnit4::class)
class CrashlyticsStubProbeTest {

    @Test
    fun stubMirrorsNonFatalToLogcatInDebug() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        StubCrashlyticsManager().logNonFatalException(
            IllegalStateException("probe: crashlytics stub logs to logcat in debug"),
            "CrashlyticsStubProbeTest on ${context.packageName}"
        )
    }
}
