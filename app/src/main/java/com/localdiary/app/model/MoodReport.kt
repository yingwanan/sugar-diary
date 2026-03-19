package com.localdiary.app.model

enum class ReportPeriod {
    DAY,
    WEEK,
    MONTH,
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
