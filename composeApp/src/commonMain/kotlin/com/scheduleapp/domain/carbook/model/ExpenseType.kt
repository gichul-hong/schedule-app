package com.scheduleapp.domain.carbook.model

enum class ExpenseType(val displayName: String, val icon: String) {
    FUEL("주유", "⛽"),
    MAINTENANCE("정비/소모품", "🔧"),
    PARKING("주차비", "🅿️"),
    TOLL("통행료", "🛣️"),
    INSURANCE("보험료", "🛡️"),
    TAX("자동차세", "🧾"),
    WASH("세차비", "🧼"),
    OTHER("기타", "💳");

    companion object {
        fun fromString(value: String): ExpenseType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}
