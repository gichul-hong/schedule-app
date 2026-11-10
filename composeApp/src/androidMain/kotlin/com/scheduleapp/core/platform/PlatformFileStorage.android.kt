package com.scheduleapp.core.platform

import android.net.Uri
import java.io.File

actual class PlatformFileStorage actual constructor() {
    private val context get() = PlatformContextHolder.appContext

    actual fun fileExists(path: String): Boolean {
        if (path.startsWith("content://")) {
            val ctx = context ?: return false
            return try {
                ctx.contentResolver.openInputStream(Uri.parse(path))?.use { true } ?: false
            } catch (_: Exception) {
                false
            }
        }
        val file = File(path)
        return file.exists() && file.length() > 0
    }

    actual fun readFileBytes(path: String): ByteArray? {
        if (path.startsWith("content://") || path.startsWith("file://")) {
            val ctx = context ?: return null
            return try {
                ctx.contentResolver.openInputStream(Uri.parse(path))?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
        }
        val file = File(path)
        return if (file.exists() && file.isFile) {
            try {
                file.readBytes()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    actual fun saveReceiptImage(fileName: String, bytes: ByteArray): String? {
        val ctx = context ?: return null
        return try {
            val dir = File(ctx.filesDir, "receipts").apply { mkdirs() }
            val destFile = File(dir, fileName)
            destFile.writeBytes(bytes)
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    actual fun getReceiptFileName(pathOrUri: String): String {
        val clean = pathOrUri.substringBefore("?").trimEnd('/')
        return clean.substringAfterLast('/')
    }
}
