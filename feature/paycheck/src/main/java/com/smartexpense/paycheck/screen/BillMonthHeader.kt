package com.smartexpense.paycheck.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.LivelyIndigo

/**
 * Month header above a group of bill rows, e.g. "Bills for November 2026",
 * with the group's count and total on the right.
 *
 * [compact] shrinks the label and its padding for dense cards such as the
 * paycheck summary.
 */
@Composable
internal fun BillMonthHeader(
    label: String,
    count: Int,
    total: Double,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (compact) 2.dp else 6.dp,
                bottom = if (compact) 1.dp else 2.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (compact) {
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            } else {
                MaterialTheme.typography.titleSmall
            },
            fontWeight = FontWeight.Bold,
            color = LivelyIndigo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count • ${total.toPesoAmount()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
