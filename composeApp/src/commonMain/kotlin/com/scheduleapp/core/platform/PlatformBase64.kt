package com.scheduleapp.core.platform

expect class PlatformBase64() {
    fun encode(bytes: ByteArray): String
    fun decode(base64Str: String): ByteArray
}
