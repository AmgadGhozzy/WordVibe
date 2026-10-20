package com.venom.stackcard.ui.components.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.venom.data.mock.MockWordData
import com.venom.domain.model.AppTheme
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordMaster
import com.venom.wordvibe.R
import com.venom.stackcard.ui.components.mastery.FrequencyWidget
import com.venom.stackcard.ui.components.mastery.InteractiveText
import com.venom.stackcard.ui.components.mastery.PowerCell
import com.venom.stackcard.ui.components.mastery.SectionHeader
import com.venom.stackcard.ui.components.mastery.StatCell
import com.venom.stackcard.ui.viewmodel.CurrentWordProgress
import com.venom.stackcard.ui.viewmodel.NextReviewBucket
import com.venom.wordvibe.R as ShellR
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.components.other.CircularProgressRing
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.core.theme.tokens.CefrColorScheme
import com.venom.wordvibe.core.theme.tokens.getCefrColorScheme

/**
 * Overview tab content for Insights sheet.
 *
 * @param word The word to display overview for
 * @param wordProgress Real user progress data for this word
 * @param showPowerTip Whether the power card flip message is shown
 * @param onTogglePowerTip Callback to toggle power tip visibility
 * @param modifier Modifier for styling
 */
@Composable
fun OverviewTab(
    word: WordMaster,
    wordProgress: CurrentWordProgress,
    showPowerTip: Boolean,
    onTogglePowerTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val cefrColors = getCefrColorScheme(word.cefrLevel)

    val hasSemanticTags by remember(word.semanticTags) {
        derivedStateOf { word.semanticTags.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.adp, vertical = 24.adp),
        verticalArrangement = Arrangement.spacedBy(20.adp)
    ) {
        // 1. YOUR PROGRESS - With REAL data
        YourProgressCard(
            repetitions = wordProgress.repetitions,
            nextReview = wordProgress.nextReviewBucket.displayLabel(wordProgress.nextReviewCount),
            masteryProgress = wordProgress.masteryProgress,
            cefrLevel = word.cefrLevel
        )

        // 2. Frequency widget
        FrequencyWidget(frequency = word.frequency)

        // 4. Semantic Cloud
        if (hasSemanticTags) {
            SemanticCloudSection(
                tags = word.semanticTags,
                scrollState = horizontalScrollState
            )
        }

        // 5. Stats Grid
        StatsGrid(
            difficulty = when (word.cefrLevel) {
                CefrLevel.A1,
                CefrLevel.A2 -> stringResource(R.string.difficulty_beginner)
                CefrLevel.B1,
                CefrLevel.B2 -> stringResource(R.string.difficulty_intermediate)
                CefrLevel.C1,
                CefrLevel.C2 -> stringResource(R.string.difficulty_advanced)
            },
            cefrLevel = word.cefrLevel.displayName,
            cefrColors = cefrColors,
            category = word.category,
            register = word.register,
            wordRank = word.rank,
            showPowerTip = showPowerTip,
            onTogglePowerTip = onTogglePowerTip
        )
    }
}

/**
 * Circular progress ring with dynamic theme color - NOW WITH REAL DATA
 */
@Composable
private fun YourProgressCard(
    repetitions: Int,
    nextReview: String,
    masteryProgress: Float,
    cefrLevel: CefrLevel,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = masteryProgress,
        animationSpec = tween(durationMillis = 1000)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.adp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                1.adp,
                MaterialTheme.colorScheme.outline.copy(0.15f),
                RoundedCornerShape(20.adp)
            )
            .padding(20.adp),
        verticalArrangement = Arrangement.spacedBy(16.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.mastery_your_progress),
            icon = painterResource(id = R.drawable.ic_trend_up)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular mastery ring
            CircularProgressRing(
                progress = animatedProgress,
                modifier = Modifier.size(80.adp),
                label = "MASTERY"
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.adp),
                color = MaterialTheme.colorScheme.outline.copy(0.2f)
            )

            // Stats
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.adp)
            ) {
                // Repetitions - REAL DATA
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = repetitions.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.mastery_repetitions).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.asp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }

                // Next review - REAL DATA
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.adp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mastery_next_review).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.asp,
                            fontSize = 9.asp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.adp))
                            .background(MaterialTheme.colorScheme.primary.copy(0.15f))
                            .padding(horizontal = 8.adp, vertical = 3.adp)
                    ) {
                        Text(
                            text = nextReview,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.asp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun SemanticCloudSection(
    tags: List<String>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.insights_semantic_cloud_title),
            icon = painterResource(id = R.drawable.ic_cloud)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            tags.forEach { tag ->
                InteractiveText(text = tag) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.adp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(
                                1.adp,
                                MaterialTheme.colorScheme.outline.copy(0.2f),
                                RoundedCornerShape(12.adp)
                            )
                            .padding(horizontal = 14.adp, vertical = 10.adp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(
    difficulty: String,
    cefrLevel: String,
    cefrColors: CefrColorScheme,
    category: String?,
    register: String,
    wordRank: Int,
    showPowerTip: Boolean,
    onTogglePowerTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.adp)
        ) {
            StatCell(
                label = stringResource(R.string.mastery_difficulty),
                watermarkIcon = R.drawable.ic_chart_line_up,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.adp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.adp))
                            .background(cefrColors.background)
                            .border(0.5.adp, cefrColors.border, RoundedCornerShape(4.adp))
                            .padding(horizontal = 6.adp, vertical = 2.adp)
                    ) {
                        Text(
                            text = cefrLevel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.asp
                            ),
                            color = cefrColors.content
                        )
                    }
                    Text(
                        text = difficulty,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.asp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PowerCell(
                showPowerTip = showPowerTip,
                onTogglePowerTip = onTogglePowerTip,
                wordRank = wordRank,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.adp)
        ) {
            category?.let {
                StatCell(
                    label = stringResource(R.string.mastery_category),
                    watermarkIcon = R.drawable.ic_briefcase,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.asp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            StatCell(
                label = stringResource(R.string.mastery_register),
                watermarkIcon = R.drawable.ic_scales,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = register,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


/** Localizes a next-review bucket; the only place bucket -> text happens. */
@Composable
private fun NextReviewBucket.displayLabel(count: Int): String = when (this) {
    NextReviewBucket.NOW -> stringResource(ShellR.string.next_review_now)
    NextReviewBucket.SOON -> stringResource(ShellR.string.next_review_soon)
    NextReviewBucket.TODAY -> stringResource(ShellR.string.next_review_today)
    NextReviewBucket.TOMORROW -> stringResource(ShellR.string.next_review_tomorrow)
    NextReviewBucket.IN_DAYS -> stringResource(ShellR.string.next_review_in_days, count)
    NextReviewBucket.IN_WEEKS -> stringResource(ShellR.string.next_review_in_weeks, count)
    NextReviewBucket.IN_MONTHS -> stringResource(ShellR.string.next_review_in_months, count)
}

// Keep your previews but update them:
@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun OverviewTabPreview() {
    WordVibeTheme(darkTheme = true) {
        OverviewTab(
            word = MockWordData.journeyWord,
            wordProgress = CurrentWordProgress(
                repetitions = 5,
                nextReviewBucket = NextReviewBucket.TOMORROW,
                masteryProgress = 0.65f
            ),
            showPowerTip = false,
            onTogglePowerTip = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun OverviewTabPreviewLight() {
    WordVibeTheme(darkTheme = false) {
        OverviewTab(
            word = MockWordData.journeyWord,
            wordProgress = CurrentWordProgress(
                repetitions = 12,
                nextReviewBucket = NextReviewBucket.IN_DAYS, nextReviewCount = 3,
                masteryProgress = 0.85f
            ),
            showPowerTip = true,
            onTogglePowerTip = {}
        )
    }
}


