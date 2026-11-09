package com.smartexpense.profile.screen

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartexpense.designsystems.*
import com.smartexpense.profile.intent.ProfileIntent
import com.smartexpense.profile.viewmodel.ProfileEffect
import com.smartexpense.profile.viewmodel.ProfileViewModel
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
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

    // Surface save/upload failures (e.g. missing storage bucket) instead of
    // leaving the avatar silently stuck as a device-only URI.
    val errorMessage = uiState.error
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage)
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
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Hero header ─────────────────────────────────────────────
            item {
                ModernHeader(
                    title = "Profile",
                    subtitle = uiState.email.takeIf { it.isNotBlank() } ?: "Manage your account",
                    icon = Icons.Default.Person
                )
            }

            // ── Identity card ────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                ProfileHeroCard(
                    avatarUri = uiState.avatarUri,
                    username = uiState.username,
                    email = uiState.email,
                    onAvatarSelected = { uri ->
                        viewModel.onIntent(ProfileIntent.AvatarChanged(uri?.toString()))
                    },
                    onAvatarRemoved = {
                        viewModel.onIntent(ProfileIntent.RemoveAvatar)
                    }
                )
            }

            // ── Personal info ────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Personal Information",
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                LivelySectionCard(
                    accent = LivelyBlue,
                    accentEnd = LivelyCyan,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    LivelyInfoRow(
                        title = "Nickname",
                        subtitle = uiState.username.takeIf { it.isNotBlank() }
                            ?: "Tap to set a nickname",
                        icon = Icons.Default.Badge,
                        accent = LivelyIndigo,
                        accentEnd = LivelyViolet,
                        onClick = { showEditNameDialog = true }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    LivelyInfoRow(
                        title = "Email Address",
                        subtitle = uiState.email.takeIf { it.isNotBlank() } ?: "Not signed in",
                        icon = Icons.Default.Email,
                        accent = LivelyBlue,
                        accentEnd = LivelyCyan,
                        onClick = null
                    )
                }
            }

            // ── Save message ─────────────────────────────────────────────
            if (uiState.saveMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(LivelyGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LivelyGreen
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = uiState.saveMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = LivelyGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Account section ──────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "Account",
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                )

                LivelySectionCard(
                    accent = LivelyOrange,
                    accentEnd = LivelyAmber,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    LivelyInfoRow(
                        title = "App Settings",
                        subtitle = "Theme, notifications & budget alerts",
                        icon = Icons.Default.Settings,
                        accent = LivelyOrange,
                        accentEnd = LivelyAmber,
                        onClick = onNavigateToSettings
                    )
                }
            }

            // ── About ───────────────────────────────────────────────────
            item {
                ModernSectionHeader(
                    title = "About",
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                LivelySectionCard(
                    accent = LivelyGreen,
                    accentEnd = LivelyTeal,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    LivelyInfoRow(
                        title = "About Spendora",
                        subtitle = "Version info, features & support",
                        icon = Icons.Default.Info,
                        accent = LivelyGreen,
                        accentEnd = LivelyTeal,
                        onClick = onNavigateToAbout
                    )
                }
            }

            // ── Sign out ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))

                LivelySectionCard(
                    accent = LivelyRose,
                    accentEnd = Color(0xFF1E3A8A),
                    title = "Session",
                    subtitle = "Log out of this device",
                    icon = Icons.Default.Logout,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    LivelyGradientButton(
                        text = "Sign Out",
                        accent = LivelyRose,
                        accentEnd = Color(0xFF1E3A8A),
                        icon = Icons.Default.Logout,
                        onClick = { showSignOutDialog = true }
                    )
                }
            }
        }
    }

    // ── Edit username dialog ────────────────────────────────────────────
    if (showEditNameDialog) {
        EditNicknameDialog(
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
private fun ProfileHeroCard(
    avatarUri: String?,
    username: String,
    email: String,
    onAvatarSelected: (Uri?) -> Unit,
    onAvatarRemoved: () -> Unit
) {
    val imageLoader = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // A null URI means the picker was cancelled — keep the current avatar.
        if (uri != null) onAvatarSelected(uri)
    }

    val avatarImage = rememberAvatarImage(avatarUri)
    val heroShape = RoundedCornerShape(28.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = heroShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            LivelyIndigo,
                            LivelyViolet,
                            LivelyPink
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .clickable { imageLoader.launch("image/*") },
                        color = Color.White.copy(alpha = 0.22f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                avatarImage != null -> Image(
                                    bitmap = avatarImage,
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                username.isNotBlank() -> Text(
                                    text = username.first().toString().uppercase(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                else -> Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { imageLoader.launch("image/*") },
                        color = Color.White,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Upload avatar",
                                modifier = Modifier.size(15.dp),
                                tint = LivelyIndigo
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = username.takeIf { it.isNotBlank() } ?: "Your name",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = email.takeIf { it.isNotBlank() } ?: "Not signed in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = { imageLoader.launch("image/*") },
                        shape = RoundedCornerShape(percent = 50),
                        color = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "Change photo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (avatarUri != null) {
                TextButton(
                    onClick = onAvatarRemoved,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Remove photo")
                }
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
private fun EditNicknameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            LivelyIconBadge(
                icon = Icons.Default.Badge,
                accent = LivelyIndigo,
                accentEnd = LivelyViolet,
                size = 44.dp,
                shape = RoundedCornerShape(14.dp)
            )
        },
        title = {
            Text(
                "Edit Nickname",
                fontWeight = FontWeight.Bold,
                color = LivelyIndigo
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose a nickname to personalize your profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LivelyIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = LivelyIndigo,
                        cursorColor = LivelyIndigo,
                        focusedContainerColor = LivelyIndigo.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LivelyIndigo,
                    contentColor = Color.White,
                    disabledContainerColor = LivelyIndigo.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(percent = 50)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = LivelyRose)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// ───────────────────────────────────────────────────────────────────────
// Avatar image loading
//
// The project does not use an image library, so decode the picked
// `content://` URI (or the uploaded `https://` URL) into a small bitmap
// off the main thread. Down-sampling keeps memory use low for large
// camera photos.
// ───────────────────────────────────────────────────────────────────────

private const val AVATAR_MAX_SIZE_PX = 512

@Composable
private fun rememberAvatarImage(avatarUri: String?): ImageBitmap? {
    val context = LocalContext.current
    // Keep the last successfully decoded image while a new URI loads, so
    // switching from the local content:// preview to the uploaded https://
    // URL (or picking another photo) doesn't flash an empty avatar.
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUri) {
        val uri = avatarUri?.takeIf { it.isNotBlank() }
        if (uri == null) {
            image = null
            return@LaunchedEffect
        }

        val loaded = withContext(Dispatchers.IO) {
            runCatching { decodeAvatar(context, Uri.parse(uri)) }.getOrNull()
        }
        if (loaded != null) {
            image = loaded
        }
    }

    return image
}

private fun decodeAvatar(context: Context, uri: Uri): ImageBitmap? {
    val bytes = when (uri.scheme?.lowercase()) {
        "content", "file", "android.resource" ->
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

        "http", "https" -> readRemoteBytes(uri.toString())

        else -> null
    } ?: return null

    if (bytes.isEmpty()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > AVATAR_MAX_SIZE_PX * 2 ||
        bounds.outHeight / sampleSize > AVATAR_MAX_SIZE_PX * 2
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory
        .decodeByteArray(bytes, 0, bytes.size, options)
        ?.asImageBitmap()
}

private fun readRemoteBytes(url: String): ByteArray? = runCatching {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    connection.setRequestProperty("Accept", "image/*")
    connection.inputStream.use { it.readBytes() }
}.getOrNull()
