package com.smartexpense.profile.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartexpense.designsystems.LivelyAmber
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyCyan
import com.smartexpense.designsystems.LivelyGreen
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyInfoRow
import com.smartexpense.designsystems.LivelyOrange
import com.smartexpense.designsystems.LivelyPink
import com.smartexpense.designsystems.LivelyRose
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyTeal
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernSectionHeader

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
        packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }

    Scaffold(
        // The header draws behind the status bar and handles its own inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Hero header ────────────────────────────────────────────────
            item {
                ModernHeader(
                    title = "About Spendora",
                    subtitle = "Smart expense tracking",
                    icon = Icons.Default.Info,
                    onBackClick = onNavigateBack
                )
            }

            // ── App hero card ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                AboutHeroCard(
                    logoRes = logoRes,
                    appName = appName,
                    version = version
                )
            }

            // ── Features ──────────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Features",
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                LivelySectionCard(
                    accent = LivelyIndigo,
                    accentEnd = LivelyViolet,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    AboutFeatureRows()
                }
            }

            // ── Information ───────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Information",
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                LivelySectionCard(
                    accent = LivelyGreen,
                    accentEnd = LivelyTeal,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    AboutInfoRows()
                }
            }

            // ── Footer ────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = LivelyRose
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Made for smarter everyday spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$appName • Version $version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHeroCard(
    @DrawableRes logoRes: Int,
    appName: String,
    version: String
) {
    val shape = RoundedCornerShape(28.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(LivelyIndigo, LivelyViolet, LivelyPink)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "Spendora logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Smart expense tracking, beautifully simple.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White
            ) {
                Text(
                    text = "Version $version",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutFeatureRows() {
    FeatureRow(
        icon = Icons.Default.Receipt,
        title = "Expense Tracking",
        subtitle = "Log and manage your transactions by category",
        accent = LivelyIndigo,
        accentEnd = LivelyViolet
    )
    FeatureDivider()
    FeatureRow(
        icon = Icons.Default.Wallet,
        title = "Budget Management",
        subtitle = "Set monthly budgets and get alert notifications",
        accent = LivelyBlue,
        accentEnd = LivelyCyan
    )
    FeatureDivider()
    FeatureRow(
        icon = Icons.Default.BarChart,
        title = "Reports & Export",
        subtitle = "Generate CSV and PDF reports for your expenses",
        accent = LivelyGreen,
        accentEnd = LivelyTeal
    )
    FeatureDivider()
    FeatureRow(
        icon = Icons.Default.Repeat,
        title = "Recurring Expenses",
        subtitle = "Automatically process recurring transactions",
        accent = LivelyOrange,
        accentEnd = LivelyAmber
    )
    FeatureDivider()
    FeatureRow(
        icon = Icons.Default.CloudUpload,
        title = "Cloud Sync",
        subtitle = "Sync your data securely with Supabase",
        accent = LivelyPink,
        accentEnd = LivelyRose
    )
}

@Composable
private fun AboutInfoRows() {
    LivelyInfoRow(
        title = "Data Privacy",
        subtitle = "Your financial data is stored locally on this device. Cloud sync is available when you are signed in.",
        icon = Icons.Default.Lock,
        accent = LivelyGreen,
        accentEnd = LivelyTeal
    )

    FeatureDivider()

    LivelyInfoRow(
        title = "Support",
        subtitle = "For questions or feedback, contact us at support@spendora.app",
        icon = Icons.Default.Email,
        accent = LivelyBlue,
        accentEnd = LivelyCyan
    )
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    accentEnd: Color
) {
    LivelyInfoRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        accent = accent,
        accentEnd = accentEnd
    )
}

@Composable
private fun FeatureDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
