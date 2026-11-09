package com.smartexpense.reports.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartexpense.designsystems.LivelyProgressBar
import com.smartexpense.domain.model.CategorySpending
import java.text.NumberFormat
import java.util.Locale

/**
 * Lively, self-contained category breakdown card.
 *
 * Categories are ordered by amount (largest first) and each row pairs an accent
 * icon chip, a tinted percentage pill and the shared animated progress bar so
 * the breakdown reads like a modern chart rather than a plain list.
 */
@Composable
fun CategorySpendingChart(
    categoryBreakdown: List<CategorySpending>,
    categoryNames: Map<Long, String>,
    modifier: Modifier = Modifier
) {
    if (categoryBreakdown.isEmpty()) {
        Text(
            text = "No category spending to display.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val palette = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        Color(0xFF60A5FA),
        Color(0xFF2563EB),
        Color(0xFF1E40AF)
    )
    val total = categoryBreakdown.sumOf { it.totalAmount }
    val sorted = categoryBreakdown.sortedByDescending { it.totalAmount }
    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, palette.first().copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.10f),
                            colorScheme.surface,
                            colorScheme.secondary.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share of spending",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${sorted.size} categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            sorted.forEachIndexed { index, category ->
                val accent = palette[index % palette.size]
                val percentage = if (total > 0.0) {
                    category.totalAmount / total * 100.0
                } else {
                    0.0
                }

                CategoryRow(
                    name = categoryNames[category.categoryId]
                        ?: "Unknown Category",
                    amount = currency.format(category.totalAmount),
                    percentage = percentage,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    name: String,
    amount: String,
    percentage: Double,
    accent: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    val chipShape = RoundedCornerShape(14.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(chipShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.75f),
                                accent
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = chipShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${"%.1f".format(Locale.US, percentage)}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }

        LivelyProgressBar(
            progress = (percentage / 100.0).toFloat(),
            color = accent,
            trackColor = accent.copy(alpha = 0.12f),
            barHeight = 10.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
