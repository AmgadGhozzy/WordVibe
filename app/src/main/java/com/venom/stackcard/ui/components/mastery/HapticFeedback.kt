package com.venom.stackcard.ui.components.mastery

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class HapticStrength {
    LIGHT,
    MEDIUM,
    STRONG
}

@Composable
fun rememberHapticFeedback(): (HapticStrength) -> Unit {
    val view = LocalView.current

    return remember(view) {
        { strength: HapticStrength ->
            performHaptic(view, strength)
        }
    }
}

private fun performHaptic(view: View, strength: HapticStrength) {
    val feedbackConstant = when (strength) {
        HapticStrength.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
        HapticStrength.MEDIUM -> HapticFeedbackConstants.LONG_PRESS
        HapticStrength.STRONG -> HapticFeedbackConstants.VIRTUAL_KEY
    }
    view.performHapticFeedback(feedbackConstant)
}

data class HapticItem(
    val name: String,
    val constant: Int
)

private val hapticItems = listOf(
    HapticItem("LONG_PRESS", HapticFeedbackConstants.LONG_PRESS),
    HapticItem("VIRTUAL_KEY", HapticFeedbackConstants.VIRTUAL_KEY),
    HapticItem("KEYBOARD_TAP", HapticFeedbackConstants.KEYBOARD_TAP),
    HapticItem("CLOCK_TICK", HapticFeedbackConstants.CLOCK_TICK),
    HapticItem("CONTEXT_CLICK", HapticFeedbackConstants.CONTEXT_CLICK),
    HapticItem("KEYBOARD_RELEASE", HapticFeedbackConstants.KEYBOARD_RELEASE),
    HapticItem("VIRTUAL_KEY_RELEASE", HapticFeedbackConstants.VIRTUAL_KEY_RELEASE),
    HapticItem("TEXT_HANDLE_MOVE", HapticFeedbackConstants.TEXT_HANDLE_MOVE),
    HapticItem("GESTURE_START", HapticFeedbackConstants.GESTURE_START),
    HapticItem("GESTURE_END", HapticFeedbackConstants.GESTURE_END),
    HapticItem("CONFIRM", HapticFeedbackConstants.CONFIRM),
    HapticItem("REJECT", HapticFeedbackConstants.REJECT),
    HapticItem("TOGGLE_ON", HapticFeedbackConstants.TOGGLE_ON),
    HapticItem("TOGGLE_OFF", HapticFeedbackConstants.TOGGLE_OFF),
    HapticItem("GESTURE_THRESHOLD_ACTIVATE", HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE),
    HapticItem(
        "GESTURE_THRESHOLD_DEACTIVATE",
        HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
    ),
    HapticItem("DRAG_START", HapticFeedbackConstants.DRAG_START),
    HapticItem("SEGMENT_TICK", HapticFeedbackConstants.SEGMENT_TICK),
    HapticItem("SEGMENT_FREQUENT_TICK", HapticFeedbackConstants.SEGMENT_FREQUENT_TICK),
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun HapticFeedbackScreen() {
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Haptic Feedback Tester") })
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(hapticItems) { item ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Button(
                            onClick = {
                                view.performHapticFeedback(item.constant)
                            }
                        ) {
                            Text("Test")
                        }
                    }
                }
            }
        }
    }
}