package com.smartexpense.reports.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexpense.domain.model.DailySpending
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Lively, self-contained daily spending card.
 *
 * Bars grow in with a short animation, use a vertical gradient, and the busiest
 * day is highlighted with a brighter fill and a glow dot. Two tinted stat chips
 * summarise the peak day and the daily average.
 */
@Composable
fun DailySpendingChart(
    dailySpending: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    if (dailySpending.isEmpty()) {
        Text(
            text = "No daily spending to display.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val primary = colorScheme.primary
    val secondary = colorScheme.secondary

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    }
    val dateFormat = remember {
        SimpleDateFormat("dd MMM", Locale.getDefault())
    }

    val maxAmount = dailySpending.maxOf { it.totalAmount }
    val peakIndex = dailySpending.indexOfFirst { it.totalAmount == maxAmount }
    val average = dailySpending.sumOf { it.totalAmount } / dailySpending.size
    val chartMax = max(maxAmount, 1.0)

    val growth = remember { Animatable(0f) }
    LaunchedEffect(dailySpending) {
        growth.snapTo(0f)
        growth.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.10f),
                            colorScheme.surface,
                            primary.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DailyStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Peak day",
                    value = currency.format(maxAmount),
                    accent = primary
                )
                DailyStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Daily average",
                    value = currency.format(average),
                    accent = secondary
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val left = 8.dp.toPx()
                val right = 8.dp.toPx()
                val top = 14.dp.toPx()
                val bottom = 26.dp.toPx()

                val chartWidth = size.width - left - right
                val chartHeight = size.height - top - bottom
                val slotWidth = chartWidth / dailySpending.size
                val barWidth = (slotWidth * 0.55f)
                    .coerceAtMost(36.dp.toPx())

                // Soft dashed gridlines for depth without visual noise.
                val gridColor = colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                val dash = PathEffect.dashPathEffect(
                    floatArrayOf(6.dp.toPx(), 6.dp.toPx())
                )
                for (step in 0..3) {
                    val y = top + chartHeight * (step / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(left, y),
                        end = Offset(size.width - right, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }

                dailySpending.forEachIndexed { index, spending ->
                    val fraction = (spending.totalAmount / chartMax)
                        .toFloat()
                        .coerceIn(0f, 1f)
                    val animatedHeight = chartHeight * fraction * growth.value
                    if (animatedHeight <= 1f) return@forEachIndexed

                    val isPeak = index == peakIndex
                    val x = left +
                            index * slotWidth +
                            (slotWidth - barWidth) / 2f
                    val y = top + chartHeight - animatedHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isPeak) {
                                listOf(
                                    primary,
                                    primary.copy(alpha = 0.45f)
                                )
                            } else {
                                listOf(
                                    secondary.copy(alpha = 0.9f),
                                    secondary.copy(alpha = 0.3f)
                                )
                            },
                            startY = y,
                            endY = y + animatedHeight
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, animatedHeight),
                        cornerRadius = CornerRadius(
                            x = barWidth / 2f,
                            y = barWidth / 2f
                        )
                    )

                    if (isPeak) {
                        // A small glow dot marks the busiest day.
                        drawCircle(
                            color = primary,
                            radius = 3.dp.toPx(),
                            center = Offset(
                                x = x + barWidth / 2f,
                                y = (y - 8.dp.toPx()).coerceAtLeast(4.dp.toPx())
                            )
                        )
                    }
                }

                // Date labels, drawn on the native canvas so they can follow the
                // same slot positions as the bars.
                drawIntoCanvas { canvas ->
                    val basePaint = android.graphics.Paint().apply {
                        color = colorScheme.onSurfaceVariant.toArgb()
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val peakPaint = android.graphics.Paint(basePaint).apply {
                        color = primary.toArgb()
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                    }

                    val labelCount = minOf(dailySpending.size, 7)
                    val indices = if (dailySpending.size <= 7) {
                        dailySpending.indices.toList()
                    } else {
                        (0 until labelCount).map { labelIndex ->
                            labelIndex * (dailySpending.size - 1) /
                                    (labelCount - 1)
                        }
                    }

                    indices.forEach { index ->
                        val x = left +
                                index * slotWidth +
                                slotWidth / 2f
                        val label = dateFormat.format(
                            Date(dailySpending[index].date)
                        )
                        canvas.nativeCanvas.drawText(
                            label,
                            x,
                            size.height - 2.dp.toPx(),
                            if (index == peakIndex) peakPaint else basePaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        accent.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.22f),
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = accent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
