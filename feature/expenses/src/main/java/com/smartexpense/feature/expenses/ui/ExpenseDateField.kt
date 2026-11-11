package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
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
    label: String = "Date",
    accentColor: Color? = null
) {
    var showPicker by remember { mutableStateOf(false) }
    val accent = accentColor ?: MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = dateMillis.toDateString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = accent
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = accent
                )
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
            modifier = Modifier.fillMaxWidth()
        )
        // Make the entire field open the calendar, not just the trailing icon.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = { showPicker = true }
                )
        )
    }

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
