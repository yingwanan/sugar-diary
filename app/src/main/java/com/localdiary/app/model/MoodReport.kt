package com.localdiary.app.model

enum class ReportPeriod {
    DAY,
    WEEK,
    MONTH,
}

val ReportPeriod.label: String
    get() = when (this) {
        ReportPeriod.DAY -> "日报"
        ReportPeriod.WEEK -> "周报"
        ReportPeriod.MONTH -> "月报"
    }

data class MoodReport(
    val id: String,
    val period: ReportPeriod,
    val rangeStart: Long,
    val rangeEnd: Long,
    val dominantMoods: List<String>,
    val averageIntensity: Int,
    val summary: String,
    val advice: List<String>,
    val createdAt: Long,
)
