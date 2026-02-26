package com.venom.wordvibe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.venom.data.model.SplashPreferences
import com.venom.data.repo.SettingsRepository
import com.venom.resources.R
import com.venom.ui.components.common.adp
import com.venom.ui.theme.LingoLensTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var keepSplashOpened = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ Use system splash screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen().setKeepOnScreenCondition { keepSplashOpened }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val prefs = settingsRepository.settings.first().splashPrefs
            val duration = prefs.splashAnimationDuration.toLong()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ Just wait to handles UI
                delay(duration)
                navigate()
            } else {
                // Android < 12 Show custom splash
                setContent {
                    LingoLensTheme {
                        SimpleSplashScreen(prefs) { navigate() }
                    }
                }
            }
        }
    }

    @Composable
    private fun SimpleSplashScreen(prefs: SplashPreferences, onFinish: () -> Unit) {

        LaunchedEffect(Unit) {
            delay(prefs.splashAnimationDuration.toLong())
            onFinish()
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(160.adp)
                    )

                    Spacer(modifier = Modifier.height(24.adp))

                    Text(
                        text = "LingoLens",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(prefs.splashTextColor)
                    )
                }
            }
        }
    }

    private fun navigate() {
        keepSplashOpened = false

        lifecycleScope.launch {
            val isFirstLaunch = settingsRepository.isFirstLaunch()

            if (isFirstLaunch) {
                settingsRepository.markFirstLaunchComplete()
            }

            startActivity(Intent(this@SplashActivity, MainActivity::class.java).apply {
                putExtra("show_onboarding", isFirstLaunch)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}