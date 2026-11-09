package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LivelyMenuShape = RoundedCornerShape(20.dp)

/**
 * A livelier version of the exposed dropdown menu container.
 *
 * Instead of the flat Material surface, the popup gets an accent-tinted gradient,
 * a rounded outline and a soft elevation shadow so it reads as a floating card.
 *
 * Must be called inside an [androidx.compose.material3.ExposedDropdownMenuBox] so
 * the popup stays anchored to the text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExposedDropdownMenuBoxScope.LivelyDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    accent: Color,
    content: @Composable () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface

    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        matchAnchorWidth = true,
        shape = LivelyMenuShape,
        containerColor = surface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surface,
                            accent.copy(alpha = 0.12f),
                            surface
                        )
                    )
                )
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

/**
 * Full-screen transparent catcher placed **behind** the dropdown popup while it is open.
 *
 * Tapping anywhere outside the menu lands on this scrim (the popup itself is a separate
 * window, so it still receives taps inside its bounds), which closes the menu and, unlike
 * a pass-through tap, does not trigger the view underneath.
 */
@Composable
internal fun DropdownDismissScrim(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}
