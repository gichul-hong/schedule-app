package com.scheduleapp.core.platform

actual class PlatformFileStorage actual constructor() {
    actual fun fileExists(path: String): Boolean = false
    actual fun readFileBytes(path: String): ByteArray? = null
    actual fun saveReceiptImage(fileName: String, bytes: ByteArray): String? = null
    actual fun getReceiptFileName(pathOrUri: String): String = pathOrUri.substringAfterLast('/')
}
