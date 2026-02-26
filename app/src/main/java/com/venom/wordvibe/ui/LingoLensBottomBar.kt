package com.venom.wordvibe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.venom.wordvibe.navigation.NavigationItems
import com.venom.wordvibe.navigation.navigateToStart
import com.venom.ui.components.common.adp
import com.venom.ui.components.other.GlassThickness
import com.venom.ui.components.other.GradientGlassCard
import com.venom.ui.navigation.Screen
import com.venom.ui.theme.lingoLens

@Composable
fun LingoLensBottomBar(
    navController: NavHostController,
    currentScreen: Screen,
    modifier: Modifier = Modifier,
) {
    val itemShape = RoundedCornerShape(14.adp)
    val selectedShape = RoundedCornerShape(18.adp)
    val visibleItems = remember { NavigationItems.entries.filter { it.showInBottomBar } }

    GradientGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .scale(0.95f)
            .offset(y = (-12).adp),
        thickness = GlassThickness.UltraThick,
        gradientAlpha = 0.1f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.adp),
            horizontalArrangement = Arrangement.spacedBy(10.adp),
        ) {
            visibleItems.forEach { item ->
                val isSelected = currentScreen == item.screen

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.adp)
                        .then(
                            if (isSelected) Modifier
                                .border(
                                    0.8.adp,
                                    MaterialTheme.colorScheme.primary.copy(0.1f),
                                    selectedShape,
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.lingoLens.glass.tintPrimary.copy(0.05f),
                                            MaterialTheme.lingoLens.glass.tintSecondary.copy(0.05f),
                                            MaterialTheme.lingoLens.glass.tintAccent.copy(0.05f),
                                        ),
                                    ),
                                    selectedShape,
                                )
                                .shadow(
                                    elevation = 24.adp,
                                    shape = itemShape,
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(0.25f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(0.4f),
                                )
                            else Modifier
                        )
                        .clip(itemShape)
                        .clickable { navController.navigateToStart(item.screen) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = stringResource(item.titleRes),
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(0.7f),
                        modifier = Modifier.size(30.adp),
                    )
                }
            }
        }
    }
}