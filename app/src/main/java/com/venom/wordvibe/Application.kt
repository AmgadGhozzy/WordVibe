package com.venom.wordvibe

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.venom.domain.provider.AppConfigProvider
import com.venom.analytics.AnalyticsManager
import com.venom.analytics.CrashlyticsManager
import com.venom.di.NetworkEntryPoint
import com.venom.ui.theme.Alexandria
import com.venom.ui.theme.Cairo
import com.venom.ui.theme.Caveat
import com.venom.ui.theme.JosefinSans
import com.venom.ui.theme.PlaypenSans
import com.venom.ui.theme.Quicksand
import com.venom.ui.theme.Roboto
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class Application : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var analyticsManager: AnalyticsManager
    @Inject
    lateinit var crashlyticsManager: CrashlyticsManager
    @Inject
    lateinit var appConfigProvider: AppConfigProvider

    override fun onCreate() {
        super.onCreate()
        
        // Initialize analytics and crashlytics FIRST
        initializeAnalytics()
        
        // Preload custom fonts (background, to prevent StrictMode violations)
        preloadFonts()
        
        // Pre-warm network clients (background, non-blocking)
        preWarmNetworkClients()

        // Enable StrictMode in debug builds only
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    private fun initializeAnalytics() {
        try {
            val enableAnalytics = appConfigProvider.isAnalyticsEnabled
            val enableCrashlytics = appConfigProvider.isCrashlyticsEnabled
            
            analyticsManager.initialize(enableAnalytics)
            crashlyticsManager.initialize(enableCrashlytics)
            
            if (enableCrashlytics) {
                crashlyticsManager.setCustomKey("app_version", BuildConfig.VERSION_NAME)
                crashlyticsManager.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            }
            
            if (enableAnalytics) {
                analyticsManager.logEvent("app_open")
            }
            
            Log.d(TAG, "Analytics ($enableAnalytics) and Crashlytics ($enableCrashlytics) initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize analytics", e)
        }
    }

    private fun preloadFonts() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val fonts = listOf(
                    Cairo,
                    Alexandria,
                    Caveat,
                    Roboto,
                    Quicksand,
                    JosefinSans,
                    PlaypenSans
                )
                Log.d(TAG, "Custom fonts preloaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload fonts", e)
                crashlyticsManager.logNonFatalException(e, "Font preloading failed")
            }
        }
    }

    private fun preWarmNetworkClients() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@Application,
                    NetworkEntryPoint::class.java
                )

                entryPoint.getRegularClient().connectionPool
                entryPoint.getAiClient().connectionPool

                Log.d(TAG, "Network clients initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize network clients", e)
                crashlyticsManager.logNonFatalException(e, "Network client initialization failed")
            }
        }
    }

    private fun enableStrictMode() {
        Log.d(TAG, "StrictMode Enabled")

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
    
    companion object {
        private const val TAG = "LingoLensApp"
    }
}