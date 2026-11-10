package com.scheduleapp.core.platform

import com.scheduleapp.core.database.dao.CarBookDao

data class SyncResult(
    val recordsCount: Int,
    val imagesCount: Int
)

class GitHubSyncEngine(
    private val httpEngine: PlatformHttpEngine,
    private val base64: PlatformBase64,
    private val backupEngine: DatabaseBackupEngine,
    private val fileStorage: PlatformFileStorage,
    private val carBookDao: CarBookDao
) {
    suspend fun testConnection(pat: String, owner: String, repo: String): Result<String> = runCatching {
        val url = "https://api.github.com/repos/$owner/$repo"
        val resp = httpEngine.get(
            url = url,
            headers = mapOf(
                "Authorization" to "Bearer $pat",
                "Accept" to "application/vnd.github.v3+json",
                "User-Agent" to "ScheduleApp-KMP"
            )
        )
        if (resp.statusCode in 200..299) {
            "저장소 연결 성공 ($owner/$repo)"
        } else {
            throw IllegalStateException("GitHub 연결 실패 (HTTP ${resp.statusCode}): ${resp.body.take(150)}")
        }
    }

    suspend fun pushBackup(
        pat: String,
        owner: String,
        repo: String,
        branch: String = "main",
        commitMessage: String = "backup: sync database snapshot via Schedule App"
    ): Result<SyncResult> = runCatching {
        val json = backupEngine.exportToJson()
        val snapshot = DatabaseBackupEngine.parseSnapshot(json)

        val filePath = "backup/schedule_db.json"
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$filePath?ref=$branch"

        val headers = mapOf(
            "Authorization" to "Bearer $pat",
            "Accept" to "application/vnd.github.v3+json",
            "User-Agent" to "ScheduleApp-KMP"
        )

        val getResp = httpEngine.get(apiUrl, headers)
        var sha: String? = null
        if (getResp.statusCode in 200..299) {
            val parsed = MiniJsonParser(getResp.body).parse() as? Map<String, Any?>
            sha = parsed?.get("sha") as? String
        }

        val encodedContent = base64.encode(json.encodeToByteArray())
        val putBody = buildString {
            append("{")
            append("\"message\": \"${commitMessage}\",")
            append("\"content\": \"${encodedContent}\",")
            append("\"branch\": \"${branch}\"")
            if (sha != null) {
                append(",\"sha\": \"${sha}\"")
            }
            append("}")
        }

        val putResp = httpEngine.put(
            url = "https://api.github.com/repos/$owner/$repo/contents/$filePath",
            headers = headers,
            body = putBody
        )

        if (putResp.statusCode !in 200..299) {
            throw IllegalStateException("GitHub 백업 업로드 실패 (HTTP ${putResp.statusCode}): ${putResp.body.take(200)}")
        }

        // Upload receipt images that are not yet on GitHub
        val receiptUris = snapshot.expenses.mapNotNull { it.receiptImageUri }.filter { it.isNotBlank() }.distinct()
        var uploadedImagesCount = 0
        for (uri in receiptUris) {
            val fileName = fileStorage.getReceiptFileName(uri)
            if (fileName.isNotBlank() && fileStorage.fileExists(uri)) {
                val remotePath = "backup/receipts/$fileName"
                val getImgUrl = "https://api.github.com/repos/$owner/$repo/contents/$remotePath?ref=$branch"
                val getImgResp = httpEngine.get(getImgUrl, headers)
                if (getImgResp.statusCode !in 200..299) {
                    val bytes = fileStorage.readFileBytes(uri)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val encodedImg = base64.encode(bytes)
                        val putImgBody = buildString {
                            append("{")
                            append("\"message\": \"backup: upload receipt image $fileName\",")
                            append("\"content\": \"${encodedImg}\",")
                            append("\"branch\": \"${branch}\"")
                            append("}")
                        }
                        val uploadImgResp = httpEngine.put(
                            url = "https://api.github.com/repos/$owner/$repo/contents/$remotePath",
                            headers = headers,
                            body = putImgBody
                        )
                        if (uploadImgResp.statusCode in 200..299) {
                            uploadedImagesCount++
                        }
                    }
                }
            }
        }

        SyncResult(
            recordsCount = snapshot.totalRecords,
            imagesCount = uploadedImagesCount
        )
    }

    suspend fun pullBackup(
        pat: String,
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<SyncResult> = runCatching {
        val filePath = "backup/schedule_db.json"
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$filePath?ref=$branch"

        val headers = mapOf(
            "Authorization" to "Bearer $pat",
            "Accept" to "application/vnd.github.v3+json",
            "User-Agent" to "ScheduleApp-KMP"
        )

        val getResp = httpEngine.get(apiUrl, headers)
        if (getResp.statusCode !in 200..299) {
            throw IllegalStateException("GitHub에서 백업 파일을 찾을 수 없습니다 (HTTP ${getResp.statusCode}). 먼저 백업을 생성해주세요.")
        }

        val parsed = MiniJsonParser(getResp.body).parse() as? Map<String, Any?>
            ?: throw IllegalStateException("GitHub 응답 파싱 실패")

        val contentBase64 = (parsed["content"] as? String)
            ?: throw IllegalStateException("GitHub 응답에 content가 없습니다.")

        val decodedBytes = base64.decode(contentBase64)
        val jsonString = decodedBytes.decodeToString()

        val restoreResult = backupEngine.restoreFromJson(jsonString)
        val snapshot = restoreResult.getOrThrow()

        // Download missing receipt images from GitHub
        var downloadedImagesCount = 0
        for (expense in snapshot.expenses) {
            val uri = expense.receiptImageUri
            if (!uri.isNullOrBlank()) {
                val fileName = fileStorage.getReceiptFileName(uri)
                if (fileName.isNotBlank() && !fileStorage.fileExists(uri)) {
                    val rawHeaders = mapOf(
                        "Authorization" to "Bearer $pat",
                        "Accept" to "application/vnd.github.v3.raw",
                        "User-Agent" to "ScheduleApp-KMP"
                    )
                    val imgUrl = "https://api.github.com/repos/$owner/$repo/contents/backup/receipts/$fileName?ref=$branch"
                    val byteResp = httpEngine.getBytes(imgUrl, rawHeaders)
                    if (byteResp.statusCode in 200..299 && byteResp.bytes.isNotEmpty()) {
                        val localPath = fileStorage.saveReceiptImage(fileName, byteResp.bytes)
                        if (localPath != null) {
                            if (localPath != expense.receiptImageUri) {
                                carBookDao.updateExpense(expense.copy(receiptImageUri = localPath))
                            }
                            downloadedImagesCount++
                        }
                    }
                }
            }
        }

        SyncResult(
            recordsCount = snapshot.totalRecords,
            imagesCount = downloadedImagesCount
        )
    }
}
