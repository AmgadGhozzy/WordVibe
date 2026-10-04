package com.venom.wordvibe.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.ui.viewmodel.SpellingQueueViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 1/2 shell smoke contracts (instrumented, real device): each tab surface
 * composes without crashing and shows its distinguishing Arabic content.
 */
@RunWith(AndroidJUnit4::class)
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
    fun homeTabComposesWithSessionCardAndAttentionRail() {
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
    fun progressTabComposesWithFourStatesAndScheduling() {
        compose.setContent {
            WordVibeTheme {
                ProgressContent(
                    isLoading = false,
                    newWordsCount = 74,
                    knownWordsCount = 11,
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
        compose.onNodeWithText("جديدة").assertExists()
        compose.onNodeWithText("تتعلّم").assertExists()
        compose.onNodeWithText("معروفة").assertExists()
        compose.onNodeWithText("متقنة").assertExists()
        compose.onNodeWithText("مستحقة").assertExists()
    }

    @Test
    fun writeTabComposesQueue() {
        compose.setContent {
            WordVibeTheme {
                SpellingQueueContent(
                    isLoading = false,
                    items = listOf(
                        SpellingQueueViewModel.QueueItem(word(1, "substantial"), isDue = true, productionWeak = true),
                        SpellingQueueViewModel.QueueItem(word(2, "inevitable"), isDue = false, productionWeak = false),
                    ),
                    selectedWord = null,
                    onPick = {},
                    onDismissEditor = {},
                )
            }
        }
        compose.onNodeWithText("طابور الكتابة — الأضعف إنتاجًا أولًا").assertIsDisplayed()
        compose.onNodeWithText("substantial").assertIsDisplayed()
        compose.onNodeWithText("inevitable").assertIsDisplayed()
    }

    @Test
    fun writeTabShowsTheArabicEmptyStateWhenTheQueueIsEmpty() {
        compose.setContent {
            WordVibeTheme {
                SpellingQueueContent(
                    isLoading = false,
                    items = emptyList(),
                    selectedWord = null,
                    onPick = {},
                    onDismissEditor = {},
                )
            }
        }
        compose.onNodeWithText("لا كلمات تنتظر التدريب").assertIsDisplayed()
    }
}
