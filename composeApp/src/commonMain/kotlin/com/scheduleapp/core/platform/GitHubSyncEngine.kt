package com.scheduleapp.core.platform

class GitHubSyncEngine(
    private val httpEngine: PlatformHttpEngine,
    private val base64: PlatformBase64,
    private val backupEngine: DatabaseBackupEngine
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
    ): Result<Int> = runCatching {
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

        if (putResp.statusCode in 200..299) {
            snapshot.totalRecords
        } else {
            throw IllegalStateException("GitHub 백업 업로드 실패 (HTTP ${putResp.statusCode}): ${putResp.body.take(200)}")
        }
    }

    suspend fun pullBackup(
        pat: String,
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<Int> = runCatching {
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
        snapshot.totalRecords
    }
}
