package com.scheduleapp.core.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

actual class PlatformHttpEngine actual constructor() {
    actual suspend fun get(url: String, headers: Map<String, String>): HttpResponseData =
        withContext(Dispatchers.IO) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { s ->
                BufferedReader(InputStreamReader(s)).readText()
            } ?: ""
            HttpResponseData(code, body)
        }

    actual suspend fun put(url: String, headers: Map<String, String>, body: String): HttpResponseData =
        withContext(Dispatchers.IO) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            connection.outputStream.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val respBody = stream?.use { s ->
                BufferedReader(InputStreamReader(s)).readText()
            } ?: ""
            HttpResponseData(code, respBody)
        }
}
