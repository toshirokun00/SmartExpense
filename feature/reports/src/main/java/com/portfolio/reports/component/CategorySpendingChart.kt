package com.portfolio.reports.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartexpense.domain.model.CategorySpending
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CategorySpendingChart(
    categoryBreakdown: List<CategorySpending>,
    categoryNames: Map<Long, String>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val colors = listOf(
        primary,
        secondary,
        tertiary,
        Color(0xFF00897B),
        Color(0xFF7E57C2),
        Color(0xFFEF6C00)
    )

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (categoryBreakdown.isEmpty()) {
            Text(
                text = "No category spending to display.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val total = categoryBreakdown.sumOf { it.totalAmount }

        // Each bar represents the percentage of the total spending.
        categoryBreakdown.forEachIndexed { index, category ->
            val color = colors[index % colors.size]
            val categoryName = categoryNames[category.categoryId]
                ?: "Unknown Category"
            val percentage = if (total > 0.0) {
                (category.totalAmount / total).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = color)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = categoryName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = NumberFormat.getCurrencyInstance(
                            Locale("en", "PH")
                        ).format(category.totalAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    drawRoundRect(
                        color = surfaceVariant,
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            size.height / 2
                        )
                    )

                    if (percentage > 0f) {
                        drawRoundRect(
                            color = color,
                            size = Size(size.width * percentage, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                size.height / 2
                            )
                        )
                    }
                }

                Text(
                    text = "${"%.1f".format(Locale.US, percentage * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
