package com.scheduleapp.core.platform

expect class PlatformHttpEngine() {
    suspend fun get(url: String, headers: Map<String, String>): HttpResponseData
    suspend fun getBytes(url: String, headers: Map<String, String>): HttpByteResponseData
    suspend fun put(url: String, headers: Map<String, String>, body: String): HttpResponseData
}

data class HttpResponseData(
    val statusCode: Int,
    val body: String
)

data class HttpByteResponseData(
    val statusCode: Int,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as HttpByteResponseData
        return statusCode == other.statusCode && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
