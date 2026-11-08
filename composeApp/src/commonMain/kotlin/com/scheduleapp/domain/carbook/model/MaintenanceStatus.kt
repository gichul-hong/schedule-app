package com.scheduleapp.domain.carbook.model

enum class MaintenanceStatus(val displayName: String) {
    OK("정상"),
    WARNING("점검 필요"),
    OVERDUE("교체 주기 초과")
}
