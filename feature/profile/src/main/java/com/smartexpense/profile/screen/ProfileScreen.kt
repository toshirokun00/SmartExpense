package com.smartexpense.profile.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartexpense.designsystems.*
import com.smartexpense.profile.intent.ProfileIntent
import com.smartexpense.profile.state.ProfileUiState
import com.smartexpense.profile.viewmodel.ProfileEffect
import com.smartexpense.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.NavigateToSettings -> onNavigateToSettings()
                is ProfileEffect.NavigateToSignIn -> onNavigateToSignIn()
                is ProfileEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Hero header ─────────────────────────────────────────────
            item {
                ModernHeader(
                    title = "Profile",
                    subtitle = uiState.email.takeIf { it.isNotBlank() } ?: "Manage your account",
                    icon = Icons.Default.Person
                )
            }

            // ── Avatar ──────────────────────────────────────────────────
            item {
                ProfileAvatarSection(
                    avatarUri = uiState.avatarUri,
                    username = uiState.username,
                    onAvatarSelected = { uri ->
                        viewModel.onIntent(ProfileIntent.AvatarChanged(uri?.toString()))
                    }
                )
            }

            // ── Personal info ────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Personal Information",
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                ProfileInfoCard(
                    title = "Username",
                    value = uiState.username.takeIf { it.isNotBlank() } ?: "Tap to set a username",
                    icon = Icons.Default.Badge,
                    onClick = { showEditNameDialog = true }
                )

                Spacer(Modifier.height(12.dp))

                ProfileInfoCard(
                    title = "Email Address",
                    value = uiState.email,
                    icon = Icons.Default.Email,
                    onClick = null
                )
            }

            // ── Save message ─────────────────────────────────────────────
            if (uiState.saveMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ModernSuccessColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = uiState.saveMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = ModernSuccessColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Account section ──────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Account",
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                ModernListItemCard(
                    title = "App Settings",
                    subtitle = "Theme, notifications & budget alerts",
                    avatarIcon = Icons.Default.Settings,
                    avatarColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToSettings,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    borderless = true
                )
            }

            // ── About ───────────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "About",
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                ModernListItemCard(
                    title = "About Spendora",
                    subtitle = "Version info, features & support",
                    avatarIcon = Icons.Default.Info,
                    avatarColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToAbout,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    borderless = true
                )
            }

            // ── Sign out ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))

                ModernListItemCard(
                    title = "Sign Out",
                    subtitle = "Log out of this device",
                    avatarIcon = Icons.Default.Logout,
                    avatarColor = ModernErrorColor,
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    borderless = true
                )
            }
        }
    }

    // ── Edit username dialog ────────────────────────────────────────────
    if (showEditNameDialog) {
        EditUsernameDialog(
            currentName = uiState.username.orEmpty(),
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                viewModel.onIntent(ProfileIntent.UsernameChanged(newName))
                showEditNameDialog = false
            }
        )
    }

    // ── Sign out confirmation dialog ────────────────────────────────────
    if (showSignOutDialog) {
        SignOutConfirmDialog(
            onDismiss = { showSignOutDialog = false },
            onConfirm = {
                showSignOutDialog = false
                viewModel.onIntent(ProfileIntent.SignOut)
            }
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// Private composables
// ───────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileAvatarSection(
    avatarUri: String?,
    username: String,
    onAvatarSelected: (Uri?) -> Unit
) {
    val imageLoader = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onAvatarSelected(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box {
            // Avatar circle
            Surface(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .clickable { imageLoader.launch("image/*") },
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (avatarUri != null) {
                        // Placeholder: in production use Coil here
                        Text(
                            text = username.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Edit badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { imageLoader.launch("image/*") },
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Upload avatar",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onClick)
                } else Modifier
            ),
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
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SignOutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                tint = ModernErrorColor
            )
        },
        title = { Text("Sign Out?") },
        text = {
            Text("You'll need to sign in again to access your expenses.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ModernErrorColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun EditUsernameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Username") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
