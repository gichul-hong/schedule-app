package com.scheduleapp.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun rememberReceiptImagePicker(onImagePicked: (String) -> Unit): () -> Unit

@Composable
expect fun ReceiptImageThumbnail(
    imageUri: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
