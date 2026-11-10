package com.scheduleapp.core.platform

actual class PlatformHttpEngine actual constructor() {
    actual suspend fun get(url: String, headers: Map<String, String>): HttpResponseData {
        return HttpResponseData(501, "Not implemented on iOS stub yet")
    }

    actual suspend fun getBytes(url: String, headers: Map<String, String>): HttpByteResponseData {
        return HttpByteResponseData(501, ByteArray(0))
    }

    actual suspend fun put(url: String, headers: Map<String, String>, body: String): HttpResponseData {
        return HttpResponseData(501, "Not implemented on iOS stub yet")
    }
}
