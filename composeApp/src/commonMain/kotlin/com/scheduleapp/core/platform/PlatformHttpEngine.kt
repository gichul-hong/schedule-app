package com.scheduleapp.core.platform

expect class PlatformHttpEngine() {
    suspend fun get(url: String, headers: Map<String, String>): HttpResponseData
    suspend fun put(url: String, headers: Map<String, String>, body: String): HttpResponseData
}

data class HttpResponseData(
    val statusCode: Int,
    val body: String
)
