package com.localdiary.app.domain.report

import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.MoodReport
import com.localdiary.app.model.ReportPeriod
import java.util.UUID

class MoodReportGenerator {
    fun generate(
        period: ReportPeriod,
        rangeStart: Long,
        rangeEnd: Long,
        analyses: List<EmotionAnalysis>,
    ): MoodReport {
        val moodCounts = analyses.flatMap { it.labels }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
            .take(3)

        val averageIntensity = if (analyses.isEmpty()) 0 else analyses.map { it.intensity }.average().toInt()
        val advice = analyses.flatMap { it.suggestions }
            .distinct()
            .take(3)
            .ifEmpty { listOf("保持记录习惯，给自己一点稳定的复盘时间。") }

        val summary = if (analyses.isEmpty()) {
            "这个周期暂无足够的心理样本。"
        } else {
            val dominant = moodCounts.firstOrNull() ?: "平稳"
            "本周期共分析 ${analyses.size} 篇文本，主导心理偏向 $dominant，平均强度约为 $averageIntensity/100。"
        }

        return MoodReport(
            id = UUID.randomUUID().toString(),
            period = period,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            dominantMoods = moodCounts,
            averageIntensity = averageIntensity,
            summary = summary,
            advice = advice,
            createdAt = System.currentTimeMillis(),
        )
    }
}
