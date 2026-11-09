package com.scheduleapp.core.platform

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberReceiptImagePicker(onImagePicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
                val destFile = File(receiptsDir, "receipt_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    onImagePicked(destFile.absolutePath)
                } else {
                    onImagePicked(uri.toString())
                }
            } catch (_: Exception) {
                onImagePicked(uri.toString())
            }
        }
    }

    return {
        launcher.launch("image/*")
    }
}

@Composable
actual fun ReceiptImageThumbnail(
    imageUri: String,
    modifier: Modifier,
    onClick: (() -> Unit)?
) {
    val context = LocalContext.current
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = imageUri) {
        value = withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                    context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                } else {
                    BitmapFactory.decodeFile(imageUri, options)
                }

                var sampleSize = 1
                val maxDim = 1280
                while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                    context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                        BitmapFactory.decodeStream(it, null, decodeOptions)
                    }
                } else {
                    BitmapFactory.decodeFile(imageUri, decodeOptions)
                }
                bitmap?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val boxModifier = modifier
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .let { mod -> if (onClick != null) mod.clickable { onClick() } else mod }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        val bmp = imageBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "영수증 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "🧾 영수증 이미지",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
