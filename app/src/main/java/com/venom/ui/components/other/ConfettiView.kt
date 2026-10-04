package com.venom.ui.components.other

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ConfettiAnimationType {
    KONFETTI
}

enum class KonfettiStyle {
    FESTIVE_BOTTOM
}

@Composable
fun ConfettiView(
    animationType: ConfettiAnimationType = ConfettiAnimationType.KONFETTI,
    konfettiStyle: KonfettiStyle = KonfettiStyle.FESTIVE_BOTTOM,
    modifier: Modifier = Modifier
) {
}
