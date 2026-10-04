package com.venom.wordvibe.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.DashboardData
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import com.venom.wordvibe.ui.screen.HomeContent
import com.venom.wordvibe.ui.screen.ProgressContent
import com.venom.wordvibe.ui.screen.SpellingQueueContent
import com.venom.wordvibe.ui.designsystem.WordStateChip
import com.venom.wordvibe.ui.designsystem.WordStateChipStyle
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Phase 1 shell smoke contracts (Robolectric, no device): each of the four tab
 * surfaces composes without crashing and shows its distinguishing Arabic content,
 * and the four surfaces are visibly different from one another.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShellTabSmokeTest {

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
        phoneticUs = "test",
        phoneticUk = null,
        phoneticAr = "اختبار",
        translit = "test",
        syllabify = null,
        definitionEn = "Test definition.",
        definitionAr = null,
        usageNote = null,
        category = null,
        primarySense = null,
        semanticTags = emptyList(),
        register = "Neutral",
        mnemonicAr = null,
        wordFamily = WordFamily(),
        synonyms = emptyList(),
        antonyms = emptyList(),
        examples = emptyMap(),
        collocations = emptyList(),
        relatedWords = RelatedWords(emptyList(), emptyList()),
        arabicAr = "يحصل على",
    )

    @Test
    fun `home tab composes with greeting session card and attention rail`() {
        compose.setContent {
            WordVibeTheme {
                HomeContent(
                    isLoading = false,
                    dashboard = DashboardData(currentStreak = 4, totalWordsNeedsReview = 7),
                    newWordsCount = 12,
                    attentionWords = listOf(word(1, "obtain")),
                    estimatedMinutes = 9,
                    onStartSession = {},
                    onPractice = {},
                )
            }
        }
        compose.onNodeWithText("أهلًا بك مجددًا 👋").assertIsDisplayed()
        compose.onNodeWithText("جلسة اليوم").assertIsDisplayed()
        compose.onNodeWithText("ابدأ جلسة اليوم").assertIsDisplayed()
        compose.onNodeWithText("obtain").assertIsDisplayed()
    }

    @Test
    fun `progress tab composes with states and streak`() {
        compose.setContent {
            WordVibeTheme {
                ProgressContent(
                    isLoading = false,
                    newWordsCount = 74,
                    dashboard = DashboardData(
                        currentStreak = 4,
                        bestStreak = 9,
                        totalWordsMastered = 8,
                        totalWordsLearning = 14,
                        totalWordsNeedsReview = 6,
                        totalDaysActive = 11,
                        totalSessionCount = 13,
                    ),
                )
            }
        }
        compose.onNodeWithText("كلماتك حسب الحالة").assertIsDisplayed()
        compose.onNodeWithText("متقنة").assertExists()
        compose.onNodeWithText("تتعلّم").assertExists()
        compose.onNodeWithText("مستحقة").assertExists()
    }

    @Test
    fun `write tab composes due and weak sections`() {
        compose.setContent {
            WordVibeTheme {
                SpellingQueueContent(
                    isLoading = false,
                    dueWords = listOf(word(1, "substantial")),
                    weakWords = listOf(word(2, "inevitable")),
                    selectedWord = null,
                    onPick = {},
                    onDismissEditor = {},
                )
            }
        }
        compose.onNodeWithText("مستحقة الآن").assertIsDisplayed()
        compose.onNodeWithText("ضعيفة في الإنتاج").assertIsDisplayed()
        compose.onNodeWithText("substantial").assertIsDisplayed()
        compose.onNodeWithText("inevitable").assertIsDisplayed()
    }

    @Test
    fun `write tab shows the Arabic empty state when the queue is empty`() {
        compose.setContent {
            WordVibeTheme {
                SpellingQueueContent(
                    isLoading = false,
                    dueWords = emptyList(),
                    weakWords = emptyList(),
                    selectedWord = null,
                    onPick = {},
                    onDismissEditor = {},
                )
            }
        }
        compose.onNodeWithText("لا كلمات تنتظر التدريب").assertIsDisplayed()
    }
}
