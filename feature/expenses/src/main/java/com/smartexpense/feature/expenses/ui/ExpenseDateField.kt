package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartexpense.core.common.toDateString

/**
 * Date-only picker for expenses. Shows a lively single-month calendar.
 */
@Composable
fun ExpenseDateField(
    dateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    var showPicker by remember { mutableStateOf(false) }
    val accent = accentColor ?: MaterialTheme.colorScheme.primary

    OutlinedTextField(
        value = dateMillis.toDateString(),
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = accent
            )
        },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date",
                    tint = accent
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = accent,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = accent,
            focusedContainerColor = accent.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )

    if (showPicker) {
        ExpenseDateCalendarDialog(
            initialDateMillis = dateMillis,
            onDismiss = { showPicker = false },
            onDateSelected = { selected ->
                onDateSelected(selected)
                showPicker = false
            }
        )
    }
}
