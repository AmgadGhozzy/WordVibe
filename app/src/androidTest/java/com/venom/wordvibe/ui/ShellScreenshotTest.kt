package com.venom.wordvibe.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.DashboardData
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.ui.screen.HomeContent
import com.venom.wordvibe.ui.screen.ProgressContent
import com.venom.wordvibe.ui.screen.SpellingQueueContent
import com.venom.wordvibe.ui.viewmodel.SpellingQueueViewModel
import androidx.compose.runtime.Composable
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.experimental.categories.Category
import java.io.File
import java.io.FileOutputStream

/**
 * Device evidence for Phase 1/2 closure: renders each tab surface in light and
 * dark and saves PNGs to the app's external files dir for adb pull. Running on
 * the API 24 emulator also proves the Cairo fallback path renders real Arabic.
 */
@RunWith(AndroidJUnit4::class)
class ShellScreenshotTest {

    private val apiLevel = android.os.Build.VERSION.SDK_INT

    @get:Rule
    val compose = createComposeRule()

    private fun word(id: Int, en: String) = WordMaster(
        id = id,
        wordEn = en,
        pos = "verb",
        cefrLevel = CefrLevel.B1,
        fromOxford = 1,
        rank = 1000 + id,
        frequency = 3,
        difficultyScore = 5,
        phoneticUs = "əbˈteɪn",
        phoneticUk = null,
        phoneticAr = "أوبتين",
        translit = "obtain",
        syllabify = "ob•tain",
        definitionEn = "To get or acquire something.",
        definitionAr = "يحصل على شيء ما",
        usageNote = null,
        category = null,
        primarySense = null,
        semanticTags = emptyList(),
        register = "Neutral",
        mnemonicAr = null,
        wordFamily = WordFamily(noun = "obtainment"),
        synonyms = listOf("acquire"),
        antonyms = listOf("lose"),
        examples = mapOf(CefrLevel.B1 to "She obtained her degree last year."),
        collocations = listOf("obtain permission"),
        relatedWords = RelatedWords(emptyList(), emptyList()),
        arabicAr = "يحصل على",
        productionExpected = "obtain",
        productionVariants = listOf("acquire"),
    )

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            WordVibeTheme(darkTheme = dark) { content() }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null)!!
        val out = File(dir, name)
        FileOutputStream(out).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val dashboard = DashboardData(
        currentStreak = 4,
        bestStreak = 9,
        totalXp = 640,
        todayXp = 45,
        levelProgress = 0.62f,
        dailyGoalProgress = 45,
        dailyGoalTarget = 50,
        totalWordsMastered = 8,
        totalWordsLearning = 14,
        totalWordsNeedsReview = 7,
        totalDaysActive = 11,
        totalSessionCount = 13,
    )

    private val queue = listOf(
        SpellingQueueViewModel.QueueItem(word(1, "substantial"), isDue = true, productionWeak = true),
        SpellingQueueViewModel.QueueItem(word(2, "inevitable"), isDue = false, productionWeak = false),
    )

    private fun shoot(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent { WordVibeTheme(darkTheme = dark) { content() } }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null)!!
        FileOutputStream(File(dir, name)).use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun homeLight() = shoot("home-light.png", false) {
        HomeContent(
            isLoading = false, dashboard = dashboard, newWordsCount = 12,
            attentionWords = listOf(word(7, "obtain")), estimatedMinutes = 9,
            onStartSession = {}, onPractice = {},
        )
    }

    @Test fun homeDark() = shoot("home-dark.png", true) {
        HomeContent(
            isLoading = false, dashboard = dashboard, newWordsCount = 12,
            attentionWords = listOf(word(7, "obtain")), estimatedMinutes = 9,
            onStartSession = {}, onPractice = {},
        )
    }

    @Test fun progressLight() = shoot("progress-light.png", false) {
        ProgressContent(isLoading = false, newWordsCount = 74, knownWordsCount = 11, dashboard = dashboard)
    }

    @Test fun progressDark() = shoot("progress-dark.png", true) {
        ProgressContent(isLoading = false, newWordsCount = 74, knownWordsCount = 11, dashboard = dashboard)
    }

    @Test fun writeQueueLight() = shoot("write-queue-light.png", false) {
        SpellingQueueContent(false, queue, null, {}, {})
    }

    @Test fun writeQueueDark() = shoot("write-queue-dark.png", true) {
        SpellingQueueContent(false, queue, null, {}, {})
    }
}
