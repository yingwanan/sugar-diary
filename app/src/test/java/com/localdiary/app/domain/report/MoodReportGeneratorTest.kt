package com.localdiary.app.domain.report

import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodReportGeneratorTest {
    private val generator = MoodReportGenerator()

    @Test
    fun `generate picks dominant moods and average intensity`() {
        val report = generator.generate(
            period = ReportPeriod.WEEK,
            rangeStart = 1L,
            rangeEnd = 10L,
            analyses = listOf(
                EmotionAnalysis(
                    id = "1",
                    entryId = "a",
                    labels = listOf("焦虑", "疲惫"),
                    intensity = 70,
                    summary = "最近状态偏紧绷",
                    suggestions = listOf("减少任务切换"),
                    safetyFlag = false,
                    createdAt = 1L,
                ),
                EmotionAnalysis(
                    id = "2",
                    entryId = "b",
                    labels = listOf("平静"),
                    intensity = 30,
                    summary = "写作时较稳定",
                    suggestions = listOf("继续保持记录"),
                    safetyFlag = false,
                    createdAt = 2L,
                ),
                EmotionAnalysis(
                    id = "3",
                    entryId = "c",
                    labels = listOf("焦虑"),
                    intensity = 90,
                    summary = "对未来有担心",
                    suggestions = listOf("先拆小任务"),
                    safetyFlag = false,
                    createdAt = 3L,
                ),
            ),
        )

        assertEquals(listOf("焦虑", "疲惫", "平静"), report.dominantMoods)
        assertEquals(63, report.averageIntensity)
        assertTrue(report.summary.contains("3 篇"))
        assertEquals(3, report.advice.size)
    }

    @Test
    fun `generate falls back when no analyses are available`() {
        val report = generator.generate(
            period = ReportPeriod.DAY,
            rangeStart = 1L,
            rangeEnd = 2L,
            analyses = emptyList(),
        )

        assertEquals(0, report.averageIntensity)
        assertTrue(report.summary.contains("暂无足够"))
        assertEquals(listOf("保持记录习惯，给自己一点稳定的复盘时间。"), report.advice)
    }
}
