package com.scheduleapp.core.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberReceiptImagePicker(onImagePicked: (String) -> Unit): () -> Unit {
    return {
        // iOS photo picker fallback placeholder URI
    }
}

@Composable
actual fun ReceiptImageThumbnail(
    imageUri: String,
    modifier: Modifier,
    onClick: (() -> Unit)?
) {
    val boxModifier = modifier
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .let { mod -> if (onClick != null) mod.clickable { onClick() } else mod }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🧾 영수증 첨부됨",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}
