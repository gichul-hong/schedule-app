package com.scheduleapp.core.platform

expect class PlatformFileStorage() {
    fun fileExists(path: String): Boolean
    fun readFileBytes(path: String): ByteArray?
    fun saveReceiptImage(fileName: String, bytes: ByteArray): String?
    fun getReceiptFileName(pathOrUri: String): String
}
