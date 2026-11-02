package com.smartexpense.feature.expenses.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class PhotoPickerHelper {
    @Composable
    fun rememberImagePickerLauncher(
        onImageSelected: (Uri?) -> Unit
    ): ActivityResultLauncher<PickVisualMediaRequest> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            onImageSelected(uri)
        }
    }
}
