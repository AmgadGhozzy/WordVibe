package com.venom.wordvibe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.venom.data.repo.SyncManager
import com.venom.domain.provider.AppConfigProvider
import com.venom.ui.screen.OnboardingScreens
import com.venom.ui.theme.LingoLensTheme
import com.venom.ui.viewmodel.OnboardingViewModel
import com.venom.ui.viewmodel.SettingsViewModel
import com.venom.utils.Extensions.showToast
import com.venom.wordvibe.ui.LingoLensApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var appConfigProvider: AppConfigProvider

    private val showOnboarding = mutableStateOf(false)

    private var currentPhotoUri: Uri? = null
    private var pendingUriCallback: ((Uri?) -> Unit)? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        pendingUriCallback?.invoke(uri)
        pendingUriCallback = null
    }

    private val documentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        pendingUriCallback?.invoke(uri)
        pendingUriCallback = null
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val resultUri = if (success) currentPhotoUri else null
        pendingUriCallback?.invoke(resultUri)
        pendingUriCallback = null
        currentPhotoUri = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showToast("Notification permission denied. Your app will not show notifications.")
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        
        lifecycle.addObserver(syncManager)

        showOnboarding.value = intent.getBooleanExtra("show_onboarding", false)

        lifecycleScope.launch {
            appConfigProvider.initialize()
        }

        setContent {
            val userPrefs = settingsViewModel.uiState.collectAsState().value
            val themePrefs = userPrefs.themePrefs
            val shouldShowOnboarding = remember { showOnboarding }

            ApplySelectedLanguage(userPrefs.appLanguage.code)

            LingoLensTheme(
                primaryColor = Color(themePrefs.primaryColor.color),
                isAmoledBlack = themePrefs.isAmoledBlack,
                materialYou = themePrefs.materialYou,
                appTheme = themePrefs.appTheme,
                colorStyle = themePrefs.colorStyle,
                fontFamilyStyle = themePrefs.fontFamily
            ) {
                if (shouldShowOnboarding.value) {
                    OnboardingScreens(
                        onGetStarted = {
                            onboardingViewModel.restoreUserProgress {
                                shouldShowOnboarding.value = false
                            }
                        },
                        onSkip = {
                            onboardingViewModel.restoreUserProgress {
                                shouldShowOnboarding.value = false
                            }
                        },
                        onGoogleSignIn = {
                            startGoogleSignIn(isFromOnboarding = true)
                        }
                    )
                } else {
                    LingoLensApp(
                        startCamera = ::startCamera,
                        imageSelector = ::selectImageFromGallery,
                        fileSelector = ::selectDocumentFromFileManager,
                        onGoogleSignIn = {
                            startGoogleSignIn(isFromOnboarding = false)
                        }
                    )
                    setupPermissions()
                }
            }
        }
    }

    @Composable
    private fun ApplySelectedLanguage(languageCode: String) {
        val locale = if (languageCode.isEmpty()) Locale.getDefault() else Locale(languageCode)
        val config = LocalConfiguration.current
        config.setLocale(locale)
        val context = LocalContext.current
        context.createConfigurationContext(config)
    }

    private fun selectImageFromGallery(callback: (Uri?) -> Unit) {
        pendingUriCallback = callback
        galleryLauncher.launch("image/*")
    }

    private fun selectDocumentFromFileManager(callback: (Uri?) -> Unit) {
        pendingUriCallback = callback
        documentLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    private fun startCamera(callback: (Uri?) -> Unit) {
        pendingUriCallback = callback
        val photoFile = createImageCacheFile()
        val photoUri = FileProvider.getUriForFile(
            this, "${packageName}.provider", photoFile
        )
        currentPhotoUri = photoUri
        cameraLauncher.launch(photoUri)
    }

    private fun createImageCacheFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PHOTO_${timeStamp}_", ".jpg", storageDir).apply {
            deleteOnExit()
        }
    }

    private fun setupPermissions() {
        askNotificationPermission()
    }

    private fun runtimeEnableAutoInit() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Firebase.messaging.isAutoInitEnabled = true
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to enable Firebase auto-init", e)
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showToast("This app needs notification permission for important updates")
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun startGoogleSignIn(isFromOnboarding: Boolean = false) {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("482771743461-er7fil93cgv5tcf9t6m28c2sahb2iium.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity,
                )

                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken

                    if (isFromOnboarding) {
                        onboardingViewModel.onGoogleSignInResult(idToken) {
                            showOnboarding.value = false
                        }
                    } else {
                        onboardingViewModel.onGoogleSignInResult(idToken) {}
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Credential Manager failed", e)
                showToast("Sign-in failed: ${e.message}")
            }
        }
    }
}