package com.scheduleapp.core.platform

actual class PlatformBase64 actual constructor() {
    actual fun encode(bytes: ByteArray): String = ""
    actual fun decode(base64Str: String): ByteArray = ByteArray(0)
}
