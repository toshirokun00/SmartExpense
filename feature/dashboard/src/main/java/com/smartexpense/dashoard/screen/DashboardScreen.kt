package com.smartexpense.dashoard.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.dashoard.intent.DashboardIntent
import com.smartexpense.dashoard.viewmodel.DashboardViewModel
import com.smartexpense.domain.model.Expense
import org.koin.androidx.compose.koinViewModel

// ---------------------------------------------------------------------------
// Local palette for dashboard specific elements.
// ---------------------------------------------------------------------------
private val SuccessColor = Color(0xFF10B981)
private val ErrorColor = Color(0xFFF43F5E)
private val WarningColor = Color(0xFFF59E0B)

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
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    DashboardHeader()
                }

                item {
                    val balance = uiState.totalBudget - uiState.totalExpense
                    HeroBalanceCard(
                        balance = balance,
                        totalBudget = uiState.totalBudget,
                        totalExpense = uiState.totalExpense,
                        progress = uiState.budgetProgress
                    )
                }

                item {
                    StatRow(
                        totalExpense = uiState.totalExpense,
                        totalBudget = uiState.totalBudget
                    )
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

                if (uiState.recentExpenses.isEmpty()) {
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
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    val hour = remember { java.time.LocalTime.now().hour }
    val greeting = when {
        hour in 5..11 -> "Good morning"
        hour in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Welcome back!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
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
    val colorScheme = MaterialTheme.colorScheme
    
    // Gradient colors for the hero card
    val gradient = Brush.linearGradient(
        colors = listOf(
            colorScheme.primary,
            Color(0xFF8B5CF6), // Purple
            Color(0xFFD946EF)  // Pinkish
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = balance.toPesoAmount(),
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(24.dp),
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
                        Text(
                            text = "Monthly Spending",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.9f) WarningColor else Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Spent",
            amount = totalExpense,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            accentColor = ErrorColor
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Budget",
            amount = totalBudget,
            icon = Icons.Default.AccountBalance,
            accentColor = SuccessColor
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount.toPesoAmount(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Expenses",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            onClick = onNavigateToExpenses
        )
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Budgets",
            icon = Icons.Default.Wallet,
            onClick = onNavigateToBudgets
        )
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Reports",
            icon = Icons.Default.BarChart,
            onClick = onNavigateToReports
        )
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Recurring",
            icon = Icons.Default.Update,
            onClick = onNavigateToRecurring
        )
    }
}

@Composable
private fun QuickActionItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RecentExpenseItem(expense: Expense) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colorScheme.secondary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { "Expense" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = expense.date.toDateString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = expense.amount.toPesoAmount(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
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
