package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smartexpense.core.common.toDateString
import com.smartexpense.designsystems.LivelyCyan
import com.smartexpense.designsystems.LivelyIconBadge
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyViolet
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Single-month range calendar.
 *
 * Shows one month at a time with previous/next navigation and a month & year
 * selector, and highlights the selected start/end range.
 */
@Composable
fun DateRangeCalendarDialog(
    initialStartDateInclusive: Long,
    initialEndDateExclusive: Long,
    onDismiss: () -> Unit,
    onRangeSelected: (startInclusive: Long, endExclusive: Long) -> Unit
) {
    val zone = ZoneId.systemDefault()

    val initialStartDate = remember(initialStartDateInclusive) {
        initialStartDateInclusive
            .takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    }
    val initialEndDate = remember(initialEndDateExclusive) {
        initialEndDateExclusive
            .takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it - 1L).atZone(zone).toLocalDate() }
    }

    var displayedMonth by remember {
        mutableStateOf(
            initialStartDate?.let { YearMonth.from(it) }
                ?: YearMonth.now(zone)
        )
    }
    var selectedStart by remember { mutableStateOf(initialStartDate) }
    var selectedEnd by remember { mutableStateOf(initialEndDate) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableIntStateOf(displayedMonth.year) }

    val hasCompleteSelection = selectedStart != null && selectedEnd != null
    val helperText = when {
        selectedStart != null && selectedEnd != null ->
            formatSelectedRange(selectedStart!!, selectedEnd!!, zone)

        selectedStart != null -> "Now pick an end date"
        else -> "Pick a start date, then an end date"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    LivelyIndigo.copy(alpha = 0.12f),
                                    LivelyViolet.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LivelyIconBadge(
                        icon = Icons.Default.DateRange,
                        accent = LivelyIndigo,
                        accentEnd = LivelyViolet,
                        size = 38.dp,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom date range",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = helperText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showMonthYearPicker) {
                    MonthYearPicker(
                        year = pickerYear,
                        displayedMonth = displayedMonth,
                        onPreviousYear = { pickerYear -= 1 },
                        onNextYear = { pickerYear += 1 },
                        onMonthSelected = { month ->
                            displayedMonth = YearMonth.of(pickerYear, month)
                            showMonthYearPicker = false
                        },
                        onBack = { showMonthYearPicker = false }
                    )
                } else {
                    MonthCalendar(
                        displayedMonth = displayedMonth,
                        selectedStart = selectedStart,
                        selectedEnd = selectedEnd,
                        onPreviousMonth = {
                            displayedMonth = displayedMonth.minusMonths(1)
                        },
                        onNextMonth = {
                            displayedMonth = displayedMonth.plusMonths(1)
                        },
                        onOpenMonthPicker = {
                            pickerYear = displayedMonth.year
                            showMonthYearPicker = true
                        },
                        onDayClick = { date ->
                            val start = selectedStart
                            val end = selectedEnd
                            when {
                                start == null || end != null -> {
                                    selectedStart = date
                                    selectedEnd = null
                                }

                                date.isBefore(start) -> {
                                    selectedStart = date
                                    selectedEnd = null
                                }

                                else -> {
                                    selectedEnd = date
                                }
                            }
                        }
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.4f
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            val start = selectedStart ?: return@Button
                            val end = selectedEnd ?: return@Button

                            onRangeSelected(
                                start
                                    .atStartOfDay(zone)
                                    .toInstant()
                                    .toEpochMilli(),
                                end
                                    .plusDays(1)
                                    .atStartOfDay(zone)
                                    .toInstant()
                                    .toEpochMilli()
                            )
                        },
                        enabled = hasCompleteSelection,
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LivelyIndigo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Apply range",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun MonthCalendar(
    displayedMonth: YearMonth,
    selectedStart: LocalDate?,
    selectedEnd: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val locale = run { val ls = LocalConfiguration.current.locales; if (ls.isEmpty) java.util.Locale.US else ls.get(0) }
    val today = LocalDate.now()
    val accent = LivelyIndigo
    val rangeColor = LivelyIndigo.copy(alpha = 0.14f)

    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val firstOfMonth = displayedMonth.atDay(1)
    val leadingEmptyCells =
        (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val daysInMonth = displayedMonth.lengthOfMonth()
    val totalCells = leadingEmptyCells + daysInMonth
    val rowCount = (totalCells + 6) / 7

    val weekdayLabels = (0..6).map { offset ->
        firstDayOfWeek.plus(offset.toLong())
            .getDisplayName(TextStyle.NARROW, locale)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = accent
                )
            }

            Spacer(Modifier.weight(1f))

            Surface(
                onClick = onOpenMonthPicker,
                shape = RoundedCornerShape(percent = 50),
                color = LivelyIndigo.copy(alpha = 0.10f),
                contentColor = LivelyIndigo
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${
                            displayedMonth.month.getDisplayName(
                                TextStyle.FULL,
                                locale
                            )
                        } ${displayedMonth.year}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select month and year",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = accent
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        repeat(rowCount) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { columnIndex ->
                    val cellIndex = rowIndex * 7 + columnIndex
                    val dayNumber = cellIndex - leadingEmptyCells + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = displayedMonth.atDay(dayNumber)
                        val isStart = date == selectedStart
                        val isEnd = date == selectedEnd
                        val isEndpoint = isStart || isEnd
                        val inRange = selectedStart != null &&
                            selectedEnd != null &&
                            date.isAfter(selectedStart) &&
                            date.isBefore(selectedEnd)
                        val isToday = date == today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .then(
                                    if (inRange) {
                                        Modifier.background(rangeColor)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = rememberAccentRipple(accent)
                                    ) { onDayClick(date) }
                                    .then(
                                        if (isEndpoint) {
                                            Modifier.background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        LivelyIndigo,
                                                        LivelyViolet
                                                    )
                                                ),
                                                CircleShape
                                            )
                                        } else if (isToday) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = LivelyViolet,
                                                shape = CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isEndpoint) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    color = when {
                                        isEndpoint -> Color.White
                                        inRange -> LivelyIndigo
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthYearPicker(
    year: Int,
    displayedMonth: YearMonth,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthSelected: (Month) -> Unit,
    onBack: () -> Unit
) {
    val locale = run { val ls = LocalConfiguration.current.locales; if (ls.isEmpty) java.util.Locale.US else ls.get(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to calendar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Select month & year",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.weight(1f))

            // Balances the back icon so the title stays centred.
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousYear) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous year",
                    tint = LivelyIndigo
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = year.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onNextYear) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next year",
                    tint = LivelyIndigo
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Month.values().toList().chunked(3).forEach { monthRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthRow.forEach { month ->
                    val isSelected = year == displayedMonth.year &&
                        month == displayedMonth.month

                    MonthChip(
                        label = month.getDisplayName(TextStyle.SHORT, locale),
                        selected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onMonthSelected(month) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MonthChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(LivelyIndigo, LivelyViolet)
                        )
                    )
                } else {
                    Modifier
                        .background(LivelyIndigo.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = LivelyIndigo.copy(alpha = 0.22f),
                            shape = shape
                        )
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun formatSelectedRange(
    startDate: LocalDate,
    endDate: LocalDate,
    zone: ZoneId
): String {
    val startLocalMillis = startDate
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    val endExclusiveMillis = endDate
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

    val start = startLocalMillis.toDateString()
    val end = (endExclusiveMillis - 1L).toDateString()

    return "$start – $end"
}

@Composable
private fun rememberAccentRipple(color: Color): Indication =
    androidx.compose.material3.ripple(color = color)
