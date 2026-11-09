package com.smartexpense.designsystems

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated, rounded progress bar with a gradient fill and a soft gloss on top.
 *
 * The value animates from zero whenever it first appears or changes, which gives
 * budget and dashboard cards a livelier, more tactile feel than the flat
 * Material indicator.
 */
@Composable
fun LivelyProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.14f),
    barHeight: Dp = 12.dp
) {
    val fraction = progress.coerceIn(0f, 1f)
    val animatedFraction = remember { Animatable(0f) }

    LaunchedEffect(fraction) {
        animatedFraction.animateTo(
            targetValue = fraction,
            animationSpec = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            )
        )
    }

    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(shape)
            .background(trackColor)
            .progressSemantics(fraction),
        contentAlignment = Alignment.CenterStart
    ) {
        if (animatedFraction.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.value)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.65f),
                                color
                            )
                        )
                    )
            ) {
                // Glossy top highlight so the fill feels tactile, not flat.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}
