package com.smartexpense.feature.expenses.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptPhotoSection(
    photoUri: String?,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Receipt Photo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (photoUri != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(photoUri) {
                        bitmap = try {
                            withContext(Dispatchers.IO) {
                                val uri = Uri.parse(photoUri!!)
                                val stream = if (uri.scheme == "http" || uri.scheme == "https") {
                                    java.net.URL(uri.toString()).openStream()
                                } else {
                                    context.contentResolver.openInputStream(uri)
                                }
                                stream?.use { s -> android.graphics.BitmapFactory.decodeStream(s) }
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val b = bitmap
                    if (b != null) {
                        Image(
                            bitmap = b.asImageBitmap(),
                            contentDescription = "Receipt photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    IconButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove photo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            Card(
                onClick = onPickPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add photo",
                                    modifier = Modifier.size(22.dp),
                                    tint = accent
                                )
                            }
                        }
                        Text(
                            text = "Add receipt photo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to pick or capture a photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
