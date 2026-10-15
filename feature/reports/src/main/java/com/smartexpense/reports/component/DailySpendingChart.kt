package com.smartexpense.reports.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.smartexpense.domain.model.DailySpending
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun DailySpendingChart(
    dailySpending: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dailySpending.isEmpty()) {
            Text(
                text = "No daily spending to display.",
                style = MaterialTheme.typography.bodyMedium,
                color = axisColor
            )
            return@Column
        }

        val maxAmount = dailySpending.maxOf { it.totalAmount }
        val chartMax = max(maxAmount, 1.0)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val leftPadding = 12.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 12.dp.toPx()
            val bottomPadding = 28.dp.toPx()

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            val slotWidth = chartWidth / dailySpending.size
            val barWidth = slotWidth * 0.6f

            drawLine(
                color = axisColor.copy(alpha = 0.35f),
                start = Offset(leftPadding, topPadding + chartHeight),
                end = Offset(size.width - rightPadding, topPadding + chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            dailySpending.forEachIndexed { index, spending ->
                val barHeight =
                    (spending.totalAmount / chartMax).toFloat() * chartHeight

                val x = leftPadding +
                        index * slotWidth +
                        (slotWidth - barWidth) / 2f

                val y = topPadding + chartHeight - barHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        x = 4.dp.toPx(),
                        y = 4.dp.toPx()
                    )
                )
            }

            // Draw date labels using the native canvas.
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                val labelCount = minOf(dailySpending.size, 7)
                val indices = if (dailySpending.size <= 7) {
                    dailySpending.indices.toList()
                } else {
                    (0 until labelCount).map { index ->
                        index * (dailySpending.size - 1) / (labelCount - 1)
                    }
                }

                indices.forEach { index ->
                    val x = leftPadding + index * slotWidth + slotWidth / 2f
                    val label = SimpleDateFormat(
                        "dd MMM",
                        Locale.getDefault()
                    ).format(Date(dailySpending[index].date))

                    canvas.nativeCanvas.drawText(
                        label,
                        x,
                        size.height - 4.dp.toPx(),
                        paint
                    )
                }
            }
        }

        Text(
            text = "Daily amounts",
            style = MaterialTheme.typography.bodySmall,
            color = axisColor
        )
    }
}