package com.smartexpense.profile.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartexpense.designsystems.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    @DrawableRes logoRes: Int
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appName = "Spendora"
    val version = try {
        packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "unknown"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── App hero ───────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "Spendora logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Version $version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Features ──────────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Features",
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                AboutListItem(
                    icon = Icons.Default.Receipt,
                    title = "Expense Tracking",
                    subtitle = "Log and manage your transactions by category"
                )

                Spacer(Modifier.height(8.dp))

                AboutListItem(
                    icon = Icons.Default.Wallet,
                    title = "Budget Management",
                    subtitle = "Set monthly budgets and get alert notifications"
                )

                Spacer(Modifier.height(8.dp))

                AboutListItem(
                    icon = Icons.Default.BarChart,
                    title = "Reports & Export",
                    subtitle = "Generate CSV and PDF reports for your expenses"
                )

                Spacer(Modifier.height(8.dp))

                AboutListItem(
                    icon = Icons.Default.Repeat,
                    title = "Recurring Expenses",
                    subtitle = "Automatically process recurring transactions"
                )

                Spacer(Modifier.height(8.dp))

                AboutListItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Cloud Sync",
                    subtitle = "Sync your data securely with Supabase"
                )
            }

            // ── Information ───────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Information",
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                AboutListItem(
                    icon = Icons.Default.Lock,
                    title = "Data Privacy",
                    subtitle = "Your financial data is stored locally on this device. Cloud sync is available when you are signed in."
                )

                Spacer(Modifier.height(8.dp))

                AboutListItem(
                    icon = Icons.Default.Email,
                    title = "Support",
                    subtitle = "For questions or feedback, contact us at support@spendora.app"
                )
            }
        }
    }
}

@Composable
private fun AboutListItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
