package com.smartexpense.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    Profile
}

/**
 * Saver for [AppTab] so tab selection survives navigation pops
 * (e.g. returning from Add Budget / Edit Budget back to Budget tab).
 */
val AppTabSaver = Saver<AppTab, String>(
    save = { it.name },
    restore = { AppTab.valueOf(it as String) }
)

private data class BottomTabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector
)

/** Shared by the ripple clip and the sliding active background. */
private val TabCornerRadius = 16.dp
private val TabShape = RoundedCornerShape(TabCornerRadius)

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
            BottomTabItem(AppTab.Profile, "Profile", Icons.Default.Person)
        )
    }

    val colorScheme = MaterialTheme.colorScheme

    val selectedIndex = tabs.indexOfFirst { it.tab == selectedTab }
        .coerceAtLeast(0)

    // Same motion as the auth screen's Sign in / Sign up segmented control:
    // one animated float slides the active background to the tapped tab.
    // animateFloatAsState retargets cleanly when taps come in mid-flight.
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(
            durationMillis = 320,
            easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        ),
        label = "TabIndicatorPosition"
    )

    // Gradient fill for the sliding background. Its footprint is the tab cell
    // itself, so it lines up exactly with the ripple shape drawn by clickable.
    val pillBrush = remember(colorScheme) {
        Brush.linearGradient(
            colors = listOf(
                colorScheme.primaryContainer,
                colorScheme.primaryContainer.copy(alpha = 0.6f)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .drawBehind {
                    val itemWidth = size.width / tabs.size

                    // The active background covers exactly one tab cell and
                    // slides between cells, matching the auth toggle's pill.
                    drawRoundRect(
                        brush = pillBrush,
                        topLeft = Offset(indicatorPosition * itemWidth, 0f),
                        size = Size(itemWidth, size.height),
                        cornerRadius = CornerRadius(TabCornerRadius.toPx())
                    )
                },
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
    val haptics = LocalHapticFeedback.current

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "TabContentColor"
    )

    // Same icon spring as the auth screen's segmented tabs.
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "TabIconScale"
    )

    Column(
        modifier = modifier
            .clip(TabShape)
            .clickable {
                if (!isSelected) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
            tint = contentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}
