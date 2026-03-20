package com.localdiary.app.domain.emotion

import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.EmotionCenterItem
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.EntryMeta
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionCenterFilterTest {
    private val zone = ZoneId.systemDefault()

    private val items = listOf(
        EmotionCenterItem(
            meta = entry("a", "春天记录", timestampOf(2026, 3, 20)),
            latestAnalysis = analysis("a", timestampOf(2026, 3, 21)),
        ),
        EmotionCenterItem(
            meta = entry("b", "工作复盘", timestampOf(2026, 3, 18)),
            latestAnalysis = analysis("b", timestampOf(2026, 3, 18)),
        ),
        EmotionCenterItem(
            meta = entry("c", "旧日记", timestampOf(2026, 2, 25)),
            latestAnalysis = null,
        ),
    )

    @Test
    fun `filter matches title query`() {
        val result = EmotionCenterFilter.filter(items, "工作")
        assertEquals(listOf("b"), result.map { it.meta.id })
    }

    @Test
    fun `filter matches exact analysis date`() {
        val result = EmotionCenterFilter.filter(items, "2026-03-21")
        assertEquals(listOf("a"), result.map { it.meta.id })
    }

    @Test
    fun `filter falls back to article date when analysis missing`() {
        val result = EmotionCenterFilter.filter(items, "2026-02")
        assertEquals(listOf("c"), result.map { it.meta.id })
    }

    private fun entry(id: String, title: String, updatedAt: Long): EntryMeta = EntryMeta(
        id = id,
        title = title,
        format = EntryFormat.MARKDOWN,
        filePath = "/tmp/$id.md",
        tags = emptyList(),
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    private fun analysis(entryId: String, createdAt: Long): EmotionAnalysis = EmotionAnalysis(
        id = "$entryId-$createdAt",
        entryId = entryId,
        labels = listOf("平静"),
        intensity = 20,
        summary = "状态稳定",
        suggestions = emptyList(),
        safetyFlag = false,
        createdAt = createdAt,
    )

    private fun timestampOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()
}
