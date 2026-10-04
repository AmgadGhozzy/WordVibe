package com.venom.stackcard.ui.components.insights

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.lerp
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.LanguageOption
import com.venom.domain.model.WordMaster
import com.venom.wordvibe.R
import com.venom.stackcard.ui.viewmodel.CurrentWordProgress
import com.venom.ui.components.buttons.CloseButton
import com.venom.ui.components.common.CustomDragHandle
import com.venom.ui.components.common.adp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private enum class SheetValue { Hidden, Expanded }

private const val SHEET_FRACTION = 0.88f

@Composable
fun InsightsSheet(
    isOpen: Boolean,
    word: WordMaster?,
    currentWordProgress: CurrentWordProgress,
    userCefrLevel: CefrLevel,
    activeTab: InsightsTab,
    pinnedLanguage: LanguageOption?,
    showPowerTip: Boolean,
    onClose: () -> Unit,
    onTabChange: (InsightsTab) -> Unit,
    onPinLanguage: (LanguageOption) -> Unit,
    onTogglePowerTip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = activeTab.ordinal,
        pageCount = { InsightsTab.entries.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            InsightsTab.entries.getOrNull(page)?.let { tab ->
                if (tab != activeTab) onTabChange(tab)
            }
        }
    }

    // Seed approximate anchors so the open animation works before onSizeChanged fires.
    val windowInfo = LocalWindowInfo.current
    val approximateHiddenPx = remember(windowInfo) {
        windowInfo.containerSize.height * SHEET_FRACTION
    }

    val sheetState = remember {
        AnchoredDraggableState(initialValue = SheetValue.Hidden).also { s ->
            s.updateAnchors(DraggableAnchors {
                SheetValue.Expanded at 0f
                SheetValue.Hidden at approximateHiddenPx
            })
        }
    }

    // React to caller-driven open/close.
    LaunchedEffect(isOpen, word) {
        when {
            isOpen && word != null -> sheetState.animateTo(SheetValue.Expanded)
            sheetState.currentValue != SheetValue.Hidden -> sheetState.animateTo(SheetValue.Hidden)
        }
    }

    // Notify parent only after the dismiss animation fully completes, which fixes
    // the reopen glitch where onClose() fired before the offset had finished resetting.
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden && isOpen) onClose()
    }

    val isVisible by remember {
        derivedStateOf {
            sheetState.currentValue != SheetValue.Hidden ||
                    sheetState.targetValue != SheetValue.Hidden
        }
    }
    if (!isVisible) return

    val scrimAlpha by remember {
        derivedStateOf {
            val hiddenAnchor = sheetState.anchors.positionOf(SheetValue.Hidden)
            if (hiddenAnchor <= 0f) return@derivedStateOf 0f
            val offset = sheetState.offset.takeUnless { it.isNaN() } ?: hiddenAnchor
            (1f - offset / hiddenAnchor).coerceIn(0f, 1f) * 0.6f
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha }
                .background(MaterialTheme.colorScheme.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        scope.launch { sheetState.animateTo(SheetValue.Hidden) }
                    }
                )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SheetContent(
                word = word,
                currentWordProgress = currentWordProgress,
                userCefrLevel = userCefrLevel,
                pagerState = pagerState,
                sheetState = sheetState,
                pinnedLanguage = pinnedLanguage,
                showPowerTip = showPowerTip,
                onClose = {
                    scope.launch { sheetState.animateTo(SheetValue.Hidden) }
                },
                onPinLanguage = onPinLanguage,
                onTogglePowerTip = onTogglePowerTip
            )
        }
    }
}

@Composable
private fun SheetContent(
    word: WordMaster?,
    currentWordProgress: CurrentWordProgress,
    userCefrLevel: CefrLevel,
    pagerState: PagerState,
    sheetState: AnchoredDraggableState<SheetValue>,
    pinnedLanguage: LanguageOption?,
    showPowerTip: Boolean,
    onClose: () -> Unit,
    onPinLanguage: (LanguageOption) -> Unit,
    onTogglePowerTip: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    // offset is NaN before the first layout pass sets real anchors.
    val sheetOffsetY by remember {
        derivedStateOf { sheetState.offset.takeUnless { it.isNaN() } ?: 0f }
    }

    // HorizontalPager swipe disabled while a vertical dismiss drag is active.
    // Only recomposes when the boolean flips, not on every drag pixel.
    val pagerScrollEnabled by remember {
        derivedStateOf { sheetOffsetY < 1f }
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = sheetState,
        positionalThreshold = { total -> total * 0.35f },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(SHEET_FRACTION)
            .onSizeChanged { size ->
                sheetState.updateAnchors(DraggableAnchors {
                    SheetValue.Expanded at 0f
                    SheetValue.Hidden at size.height.toFloat()
                })
            }
            .graphicsLayer { translationY = sheetOffsetY }
            .clip(RoundedCornerShape(topStart = 28.adp, topEnd = 28.adp))
            .background(MaterialTheme.colorScheme.surface)
            .anchoredDraggable(
                state = sheetState,
                orientation = Orientation.Vertical,
                flingBehavior = flingBehavior
            )
    ) {
        CustomDragHandle(modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.adp, vertical = 4.adp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.mastery_insights),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            CloseButton(onClose)
        }

        // pagerState is the single source of truth — tabs read from it, not from the parent prop.
        val activeTabFromPager by remember {
            derivedStateOf {
                InsightsTab.entries.getOrElse(pagerState.currentPage) { InsightsTab.OVERVIEW }
            }
        }

        InsightsTabs(
            activeTab = activeTabFromPager,
            onTabChange = { tab ->
                coroutineScope.launch { pagerState.animateScrollToPage(tab.ordinal) }
            },
            modifier = Modifier.padding(horizontal = 24.adp, vertical = 8.adp)
        )

        word?.let { currentWord ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                beyondViewportPageCount = 1,
                userScrollEnabled = pagerScrollEnabled,
                key = { page -> page }
            ) { page ->
                val pageOffset =
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = lerp(0.5f, 1f, fraction)
                            scaleX = lerp(0.95f, 1f, fraction)
                            scaleY = lerp(0.95f, 1f, fraction)
                        }
                ) {
                    when (InsightsTab.entries[page]) {
                        InsightsTab.OVERVIEW -> OverviewTab(
                            word = currentWord,
                            showPowerTip = showPowerTip,
                            onTogglePowerTip = onTogglePowerTip,
                            wordProgress = currentWordProgress
                        )

                        InsightsTab.RELATIONS -> RelationsTab(word = currentWord)
                        InsightsTab.USAGE -> UsageTab(
                            word = currentWord,
                            userCefrLevel = userCefrLevel
                        )

                        InsightsTab.LANGUAGES -> LanguagesTab(
                            word = currentWord,
                            pinnedLanguage = pinnedLanguage,
                            onPinLanguage = onPinLanguage
                        )
                    }
                }
            }
        }
    }
}
