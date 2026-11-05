package com.smartexpense.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Destination tabs managed by the bottom navigation shell.
 */
enum class AppTab {
    Home,
    Expenses,
    Reports,
    Budget,
    Settings
}

private data class BottomTabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector
)

@Composable
fun ModernBottomNavigation(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            BottomTabItem(AppTab.Home, "Home", Icons.Default.Home),
            BottomTabItem(AppTab.Expenses, "Expenses", Icons.Default.Payment),
            BottomTabItem(AppTab.Reports, "Reports", Icons.Default.BarChart),
            BottomTabItem(AppTab.Budget, "Budget", Icons.Default.Wallet),
            BottomTabItem(AppTab.Settings, "Settings", Icons.Default.Settings)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                BottomTabItemContent(
                    item = item,
                    isSelected = selectedTab == item.tab,
                    onClick = { onTabSelected(item.tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomTabItemContent(
    item: BottomTabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "TabContentColor"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "TabContainerColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.9f,
        animationSpec = tween(durationMillis = 300),
        label = "TabScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(containerColor, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}
