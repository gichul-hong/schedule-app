package com.scheduleapp.core.platform

import android.util.Base64

actual class PlatformBase64 actual constructor() {
    actual fun encode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    actual fun decode(base64Str: String): ByteArray {
        val clean = base64Str.replace("\n", "").replace("\r", "")
        return Base64.decode(clean, Base64.DEFAULT)
    }
}
