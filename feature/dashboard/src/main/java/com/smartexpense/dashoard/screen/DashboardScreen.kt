package com.smartexpense.dashoard.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.dashoard.intent.DashboardIntent
import com.smartexpense.dashoard.viewmodel.DashboardViewModel
import com.smartexpense.designsystems.LivelyProgressBar
import com.smartexpense.domain.model.BudgetInsight
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseSyncStatus
import org.koin.androidx.compose.koinViewModel

// ---------------------------------------------------------------------------
// Local palette for dashboard specific elements.
// ---------------------------------------------------------------------------
private val SuccessColor = Color(0xFF3B82F6)
private val ErrorColor = Color(0xFF1E3A8A)
private val WarningColor = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToExpenses: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToRecurring: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.onIntent(DashboardIntent.LoadDashboard)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
     ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                DashboardHeader(
                    username = uiState.username
                )
            }

            item {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(224.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(shimmerBrush())
                    )
                } else {
                    val balance = uiState.totalBudget - uiState.totalExpense
                    HeroBalanceCard(
                        balance = balance,
                        totalBudget = uiState.totalBudget,
                        totalExpense = uiState.totalExpense,
                        progress = uiState.budgetProgress
                    )
                }
            }

            item {
                if (uiState.isLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(shimmerBrush())
                            )
                        }
                    }
                } else {
                    StatRow(
                        totalExpense = uiState.totalExpense,
                        totalBudget = uiState.totalBudget
                    )
                }
            }

            if (!uiState.isLoading && uiState.insights.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Insights",
                        onSeeAllClick = null
                    )
                }
                items(
                    items = uiState.insights,
                    key = { it.dashboardKey() }
                ) { insight ->
                    DashboardInsightCard(insight = insight)
                }
            }

            item {
                SectionHeader(
                    title = "Quick Actions",
                    onSeeAllClick = null
                )
                Spacer(modifier = Modifier.height(12.dp))
                QuickActionsGrid(
                    onNavigateToExpenses = onNavigateToExpenses,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToReports = onNavigateToReports,
                    onNavigateToRecurring = onNavigateToRecurring
                )
            }

            item {
                SectionHeader(
                    title = "Recent Expenses",
                    onSeeAllClick = onNavigateToTransactions
                )
            }

            if (uiState.isLoading) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(shimmerBrush())
                    )
                }
            } else if (uiState.recentExpenses.isEmpty()) {
                item {
                    EmptyRecentState()
                }
            } else {
                items(
                    items = uiState.recentExpenses,
                    key = { it.id }
                ) { expense ->
                    RecentExpenseItem(expense = expense)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    username: String?
) {
    val hour = remember { java.time.LocalTime.now().hour }
    val greeting = when {
        hour in 5..11 -> "Good morning"
        hour in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    val displayName = username?.takeIf { it.isNotBlank() }

    Column {
        Text(
            text = "$greeting${if (displayName != null) ", $displayName" else ""}!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeroBalanceCard(
    balance: Double,
    totalBudget: Double,
    totalExpense: Double,
    progress: Float
) {
    // Deep brand tones in both themes: white text needs a saturated surface,
    // so this deliberately avoids the lighter dark-mode `primary`.
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2563EB), // Indigo
            Color(0xFF1D4ED8), // Blue
            Color(0xFF60A5FA)  // Pinkish
        )
    )
    val heroShape = RoundedCornerShape(28.dp)
    val isOverBudget = progress > 0.9f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp),
        shape = heroShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Soft floating blobs add depth without extra imagery.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 48.dp, y = (-56).dp)
                    .size(180.dp)
                    .background(Color.White.copy(alpha = 0.10f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-52).dp, y = 64.dp)
                    .size(150.dp)
                    .background(Color.White.copy(alpha = 0.07f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "TOTAL BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = balance.toPesoAmount(),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Spending",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LivelyProgressBar(
                            progress = progress,
                            color = if (isOverBudget) WarningColor else Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            barHeight = 10.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = totalExpense.toPesoAmount(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "of ${totalBudget.toPesoAmount()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    totalExpense: Double,
    totalBudget: Double
) {
    val rawUsage = if (totalBudget > 0.0) {
        totalExpense / totalBudget
    } else {
        0.0
    }
    val usage = rawUsage.toFloat().coerceIn(0f, 1f)
    val remaining = (totalBudget - totalExpense).coerceAtLeast(0.0)
    val remainingFraction = if (totalBudget > 0.0) {
        (remaining / totalBudget).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Spent",
            amount = totalExpense,
            caption = if (totalBudget > 0.0) {
                "${(rawUsage * 100).toInt()}% of budget"
            } else {
                "No budget set"
            },
            progress = usage,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            accentColor = ErrorColor,
            accentEndColor = Color(0xFF1D4ED8)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Budget",
            amount = totalBudget,
            caption = if (totalBudget > 0.0) {
                "${remaining.toPesoAmount()} left"
            } else {
                "Set a budget"
            },
            progress = remainingFraction,
            icon = Icons.Default.AccountBalance,
            accentColor = SuccessColor,
            accentEndColor = Color(0xFF93C5FD)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    caption: String,
    progress: Float,
    icon: ImageVector,
    accentColor: Color,
    accentEndColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    val chipShape = RoundedCornerShape(14.dp)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f),
                            colorScheme.surface,
                            accentColor.copy(alpha = 0.03f)
                        )
                    )
                )
                .padding(16.dp)
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
                                    accentColor.copy(alpha = 0.75f),
                                    accentEndColor
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
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = amount.toPesoAmount(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            LivelyProgressBar(
                progress = progress,
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.14f),
                barHeight = 8.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardInsightCard(insight: BudgetInsight) {
    val colorScheme = MaterialTheme.colorScheme

    val accent = when (insight) {
        is BudgetInsight.ApproachingLimit -> WarningColor
        is BudgetInsight.ExceededLimit -> ErrorColor
        is BudgetInsight.SpendingTrend ->
            if (insight.isIncrease) ErrorColor else SuccessColor
        is BudgetInsight.CategorySpendingChange -> colorScheme.tertiary
    }

    val icon = when (insight) {
        is BudgetInsight.ApproachingLimit -> Icons.Default.WarningAmber
        is BudgetInsight.ExceededLimit -> Icons.Default.ErrorOutline
        is BudgetInsight.SpendingTrend ->
            if (insight.isIncrease) {
                Icons.AutoMirrored.Filled.TrendingUp
            } else {
                Icons.AutoMirrored.Filled.TrendingDown
            }
        is BudgetInsight.CategorySpendingChange -> Icons.Default.Category
    }

    val title = when (insight) {
        is BudgetInsight.ApproachingLimit -> "Approaching limit"
        is BudgetInsight.ExceededLimit -> "Over budget"
        is BudgetInsight.SpendingTrend -> "Spending trend"
        is BudgetInsight.CategorySpendingChange -> insight.categoryName
    }

    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.14f),
                            accent.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.dashboardMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (onSeeAllClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    )
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onNavigateToExpenses: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToRecurring: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.10f),
                            colorScheme.surface,
                            colorScheme.tertiary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "Expenses",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                accent = Color(0xFF2563EB),
                onClick = onNavigateToExpenses
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "Budgets",
                icon = Icons.Default.Wallet,
                accent = Color(0xFF3B82F6),
                onClick = onNavigateToBudgets
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "Reports",
                icon = Icons.Default.BarChart,
                accent = Color(0xFF2563EB),
                onClick = onNavigateToReports
            )
            QuickActionItem(
                modifier = Modifier.weight(1f),
                label = "Recurring",
                icon = Icons.Default.Update,
                accent = Color(0xFF1E40AF),
                onClick = onNavigateToRecurring
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "quickActionScale"
    )
    val tileShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(tileShape)
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
                    color = Color.White.copy(alpha = 0.22f),
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(25.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun RecentExpenseItem(expense: Expense) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = when (expense.syncStatus) {
        ExpenseSyncStatus.SYNCED -> SuccessColor
        ExpenseSyncStatus.FAILED -> colorScheme.error
        else -> colorScheme.primary
    }
    val syncLabel = when (expense.syncStatus) {
        ExpenseSyncStatus.SYNCED -> "Synced"
        ExpenseSyncStatus.FAILED -> "Failed"
        else -> "Pending"
    }
    val chipShape = RoundedCornerShape(15.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.10f),
                            colorScheme.surface,
                            accent.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { "Expense" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.date.toDateString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = syncLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = accent
                    )
                }
            }

            Text(
                text = expense.amount.toPesoAmount(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyRecentState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No recent expenses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun BudgetInsight.dashboardKey(): String {
    return when (this) {
        is BudgetInsight.ApproachingLimit -> "approaching-$budgetId"
        is BudgetInsight.ExceededLimit -> "exceeded-$budgetId"
        is BudgetInsight.SpendingTrend -> "trend"
        is BudgetInsight.CategorySpendingChange -> "category-$categoryId"
    }
}

private fun BudgetInsight.dashboardMessage(): String {
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


@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        ),
        start = Offset(progress * 600f, 0f),
        end = Offset(progress * 600f + 300f, 0f)
    )
}
