package com.smartexpense.budget.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.budget.effect.BudgetEffect
import com.smartexpense.budget.intent.BudgetIntent
import com.smartexpense.budget.state.BudgetSummaryState
import com.smartexpense.budget.viewmodel.BudgetViewModel
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.*
import com.smartexpense.domain.model.BudgetInsight
import com.smartexpense.domain.model.BudgetStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = koinViewModel(),
    onAddBudgetClick: () -> Unit,
    onEditBudgetClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            val message = when (effect) {
                is BudgetEffect.ShowError -> effect.message
                BudgetEffect.BudgetAdded -> "Budget added"
                BudgetEffect.BudgetUpdated -> "Budget updated"
                BudgetEffect.BudgetDeleted -> "Budget deleted"
            }

            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ModernHeader(
                title = "Budgets",
                subtitle = "Set limits and track spending by category",
                icon = Icons.Default.Wallet,
                actions = {
                    val colorScheme = MaterialTheme.colorScheme
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        colorScheme.primary,
                                        colorScheme.secondary
                                    )
                                )
                            )
                            .clickable(enabled = true, onClick = onAddBudgetClick)
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Add Budget",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    ModernSectionHeader(title = "Your Budgets")
                }

                items(
                    items = uiState.budgetSummaries,
                    key = {
                        it.budget.id
                    }
                ) { summary ->
                    ModernBudgetCard(
                        summary = summary,
                        onEdit = {
                            onEditBudgetClick(summary.budget.id)
                        },
                        onDelete = {
                            viewModel.onIntent(
                                BudgetIntent.RequestDeleteBudget(
                                    summary.budget.id
                                )
                            )
                        }
                    )
                }

                if (uiState.insights.isNotEmpty()) {
                    item {
                        ModernSectionHeader(title = "Insights")
                    }
                    items(
                        items = uiState.insights,
                        key = { insight -> insight.key() }
                    ) { insight ->
                        ModernInsightCard(insight = insight)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Delete Confirmation
    if (screenState.showDeleteConfirmation) {

        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(
                    BudgetIntent.CancelDeleteBudget
                )
            },
            title = {
                Text("Delete Budget")
            },
            text = {
                Text(
                    "Are you sure you want to delete this budget?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        viewModel.onIntent(
                            BudgetIntent.ConfirmDeleteBudget
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        viewModel.onIntent(
                            BudgetIntent.CancelDeleteBudget
                        )
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BudgetSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accent
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun ModernBudgetCard(
    summary: BudgetSummaryState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val progressColor = when {
        summary.isOverBudget -> ModernErrorColor
        summary.progress > 0.8f -> ModernWarningColor
        else -> ModernSuccessColor
    }

    val gradient = remember(colorScheme.secondaryContainer, progressColor) {
        Brush.linearGradient(
            colors = listOf(
                colorScheme.secondaryContainer.copy(alpha = 0.95f),
                progressColor.copy(alpha = 0.4f)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, progressColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = progressColor.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = progressColor
                        )
                    }
                }

                Column {
                    Text(
                        text = summary.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary.budget.startDate.toDateString() + " - " + summary.budget.endDate.toDateString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = colorScheme.onSurface.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.budget.amount.toPesoAmount(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.spentAmount.toPesoAmount(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = colorScheme.onSurface.copy(alpha = 0.15f)
            )

            Text(
                text = if (summary.isOverBudget) {
                    "Over by ${(-summary.remainingAmount).toPesoAmount()}"
                } else {
                    "Remaining ${summary.remainingAmount.toPesoAmount()}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = progressColor
            )

            LivelyProgressBar(
                progress = summary.progress,
                color = progressColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Text(
                text = "${summary.utilizationPercentage.toInt()}% used" +
                        summary.status.indicatorSuffix(),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BudgetActionButton(
                    label = "Edit",
                    icon = Icons.Default.Edit,
                    background = Brush.horizontalGradient(
                        colors = listOf(
                            colorScheme.primary,
                            colorScheme.secondary
                        )
                    ),
                    contentColor = Color.White,
                    borderColor = colorScheme.primary.copy(alpha = 0.35f),
                    glowColor = colorScheme.primary,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )

                BudgetActionButton(
                    label = "Delete",
                    icon = Icons.Default.Delete,
                    background = Brush.horizontalGradient(
                        colors = listOf(
                            colorScheme.error.copy(alpha = 0.16f),
                            colorScheme.error.copy(alpha = 0.06f)
                        )
                    ),
                    contentColor = colorScheme.error,
                    borderColor = colorScheme.error.copy(alpha = 0.35f),
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Pill-shaped budget action with a soft press animation.
 *
 * A vivid gradient is reserved for the primary action (Edit); the destructive
 * action (Delete) uses a tinted, low-emphasis container so it reads as
 * secondary and is harder to hit by accident — the recommended hierarchy for a
 * finance app.
 */
@Composable
private fun BudgetActionButton(
    label: String,
    icon: ImageVector,
    background: Brush,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "budgetActionScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (glowColor != null) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = shape,
                        ambientColor = glowColor.copy(alpha = 0.45f),
                        spotColor = glowColor.copy(alpha = 0.45f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(brush = background)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                onClick = onClick
            )
            .heightIn(min = 46.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ModernInsightCard(insight: BudgetInsight) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme.secondaryContainer, colorScheme.tertiaryContainer) {
        Brush.horizontalGradient(
            colors = listOf(
                colorScheme.secondaryContainer.copy(alpha = 0.9f),
                colorScheme.tertiaryContainer.copy(alpha = 0.8f)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = insight.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun BudgetStatus.indicatorSuffix(): String {
    return when (this) {
        BudgetStatus.WITHIN_BUDGET -> ""
        BudgetStatus.APPROACHING_LIMIT -> " • Approaching limit"
        BudgetStatus.EXCEEDED -> " • Exceeded limit"
    }
}

private fun BudgetInsight.key(): String {
    return when (this) {
        is BudgetInsight.ApproachingLimit -> "approaching-$budgetId"
        is BudgetInsight.ExceededLimit -> "exceeded-$budgetId"
        is BudgetInsight.SpendingTrend -> "trend"
        is BudgetInsight.CategorySpendingChange -> "category-$categoryId"
    }
}

private fun BudgetInsight.message(): String {
    return when (this) {
        is BudgetInsight.ApproachingLimit ->
            "You've used ${utilizationPercentage.toInt()}% of your " +
                "$categoryName budget. ${remainingAmount.toPesoAmount()} remaining."

        is BudgetInsight.ExceededLimit ->
            "You're over your $categoryName budget by " +
                "${overAmount.toPesoAmount()} (${utilizationPercentage.toInt()}% used)."

        is BudgetInsight.SpendingTrend -> {
            val direction = if (isIncrease) "up" else "down"
            "Spending is $direction ${kotlin.math.abs(changePercentage).toInt()}% " +
                "versus the previous period " +
                "(${currentAmount.toPesoAmount()} vs ${previousAmount.toPesoAmount()})."
        }

        is BudgetInsight.CategorySpendingChange ->
            "$categoryName spending is up ${changePercentage.toInt()}% " +
                "versus the previous period " +
                "(${currentAmount.toPesoAmount()} vs ${previousAmount.toPesoAmount()})."
    }
}
