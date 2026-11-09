package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexpense.designsystems.MoneyAmountField
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.domain.model.ExpenseType
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.feature.expenses.state.ExpenseFormState

private val FormPrimary = Color(0xFF2563EB)
private val FormPrimaryEnd = Color(0xFF60A5FA)
private val FormSuccess = Color(0xFF3B82F6)
private val FormWarning = Color(0xFF2563EB)
private val FormError = Color(0xFF1E3A8A)

private val FormFieldShape = RoundedCornerShape(14.dp)

/**
 * Shared, lively form used by both the Add and Edit expense screens.
 *
 * The amount sits in a gradient hero card with expense-type pills, while the
 * remaining fields are grouped into accent-tinted section cards so the screen
 * reads as a set of distinct, modern steps instead of one long grey form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseFormContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    form: ExpenseFormState,
    categories: List<Category>,
    submitLabel: String,
    onBackClick: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onExpenseTypeChanged: (ExpenseType) -> Unit,
    onCategoryChanged: (Long) -> Unit,
    onPaymentMethodChanged: (PaymentMethod) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onTagsChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDateChanged: (Long) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    syncStatus: ExpenseSyncStatus? = null,
    onRetrySync: (() -> Unit)? = null
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    val categoryAccent = MaterialTheme.colorScheme.tertiary
    val paymentAccent = MaterialTheme.colorScheme.secondary

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ModernHeader(
                title = title,
                subtitle = subtitle,
                icon = icon,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AmountHeroCard(
                    amount = form.amount,
                    expenseType = form.expenseType,
                    onAmountChanged = onAmountChanged,
                    onExpenseTypeChanged = onExpenseTypeChanged
                )

                FormSectionCard(
                    title = "Details",
                    icon = Icons.Default.Category,
                    accent = categoryAccent
                ) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = {
                            categoryExpanded = !categoryExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = categories
                                .find { it.id == form.categoryId }
                                ?.name
                                ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = categoryExpanded
                                )
                            },
                            colors = expenseFieldColors(categoryAccent),
                            shape = FormFieldShape,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        LivelyDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = {
                                categoryExpanded = false
                            },
                            accent = categoryAccent
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        onCategoryChanged(category.id)
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = paymentMethodExpanded,
                        onExpandedChange = {
                            paymentMethodExpanded = !paymentMethodExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = form.paymentMethod.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = paymentMethodExpanded
                                )
                            },
                            colors = expenseFieldColors(paymentAccent),
                            shape = FormFieldShape,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        LivelyDropdownMenu(
                            expanded = paymentMethodExpanded,
                            onDismissRequest = {
                                paymentMethodExpanded = false
                            },
                            accent = paymentAccent
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.displayName) },
                                    onClick = {
                                        onPaymentMethodChanged(method)
                                        paymentMethodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExpenseTextField(
                        value = form.description,
                        onValueChange = onDescriptionChanged,
                        label = "Description",
                        accent = FormPrimary
                    )

                    ExpenseTextField(
                        value = form.merchant,
                        onValueChange = onMerchantChanged,
                        label = "Merchant / Payee",
                        accent = FormPrimary
                    )
                }

                FormSectionCard(
                    title = "More details",
                    icon = Icons.Default.DateRange,
                    accent = paymentAccent
                ) {
                    ExpenseDateField(
                        dateMillis = form.date,
                        onDateSelected = onDateChanged,
                        accentColor = paymentAccent
                    )

                    ExpenseTextField(
                        value = form.tags,
                        onValueChange = onTagsChanged,
                        label = "Tags (comma separated, e.g. work, trip)",
                        accent = paymentAccent
                    )

                    ExpenseTextField(
                        value = form.notes,
                        onValueChange = onNotesChanged,
                        label = "Notes",
                        accent = paymentAccent,
                        minLines = 2
                    )

                    ReceiptPhotoSection(
                        photoUri = form.receiptPhotoUri,
                        onPickPhoto = onPickPhoto,
                        onRemovePhoto = onRemovePhoto
                    )
                }

                if (syncStatus != null && onRetrySync != null) {
                    SyncStatusCard(
                        status = syncStatus,
                        onRetry = onRetrySync
                    )
                }

                GradientSubmitButton(
                    label = submitLabel,
                    isSaving = form.isSaving,
                    onClick = onSubmit
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        DropdownDismissScrim(
            visible = categoryExpanded || paymentMethodExpanded,
            onDismiss = {
                categoryExpanded = false
                paymentMethodExpanded = false
            }
        )
    }
}

@Composable
private fun AmountHeroCard(
    amount: String,
    expenseType: ExpenseType,
    onAmountChanged: (String) -> Unit,
    onExpenseTypeChanged: (ExpenseType) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val chipShape = RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, FormPrimary.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            FormPrimary.copy(alpha = 0.16f),
                            colorScheme.surface,
                            colorScheme.tertiary.copy(alpha = 0.07f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(chipShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    FormPrimary.copy(alpha = 0.75f),
                                    FormPrimaryEnd
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
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = FormPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MoneyAmountField(
                value = amount,
                onValueChange = onAmountChanged,
                label = "Amount",
                prefix = "₱",
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                colors = expenseFieldColors(FormPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Expense type",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseType.entries.forEach { type ->
                    ExpenseTypePill(
                        modifier = Modifier.weight(1f),
                        label = type.displayName,
                        selected = type == expenseType,
                        onClick = { onExpenseTypeChanged(type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseTypePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(FormPrimary, FormPrimaryEnd)
                        )
                    )
                } else {
                    Modifier
                        .background(colorScheme.surface.copy(alpha = 0.7f))
                        .border(
                            width = 1.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.6f),
                            shape = shape
                        )
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                Color.White
            } else {
                colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.10f),
                            colorScheme.surface,
                            accent.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = accent
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun ExpenseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        colors = expenseFieldColors(accent),
        shape = FormFieldShape,
        minLines = minLines,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SyncStatusCard(
    status: ExpenseSyncStatus,
    onRetry: () -> Unit
) {
    val accent = when (status) {
        ExpenseSyncStatus.SYNCED -> FormSuccess
        ExpenseSyncStatus.FAILED -> FormError
        ExpenseSyncStatus.PENDING -> FormWarning
    }
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.12f),
                            accent.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sync status: ${status.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (status != ExpenseSyncStatus.SYNCED) {
                Text(
                    text = "Sync Now",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun GradientSubmitButton(
    label: String,
    isSaving: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val gradient = if (isSaving) {
        Brush.horizontalGradient(
            colors = listOf(
                FormPrimary.copy(alpha = 0.55f),
                FormPrimaryEnd.copy(alpha = 0.55f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(FormPrimary, FormPrimaryEnd)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(gradient)
            .clickable(enabled = !isSaving, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun expenseFieldColors(accent: Color): TextFieldColors {
    val colorScheme = MaterialTheme.colorScheme

    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        unfocusedBorderColor = accent.copy(alpha = 0.28f),
        focusedLabelColor = accent,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        cursorColor = accent,
        focusedContainerColor = accent.copy(alpha = 0.05f),
        unfocusedContainerColor = Color.Transparent,
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface
    )
}
