package com.localdiary.app.domain.emotion

import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.EntryMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmotionCenterProjectorTest {
    @Test
    fun `project keeps latest analysis per entry and sorts by analysis time first`() {
        val first = entry(id = "first", updatedAt = 100L)
        val second = entry(id = "second", updatedAt = 200L)
        val third = entry(id = "third", updatedAt = 300L)

        val items = EmotionCenterProjector.project(
            entries = listOf(first, second, third),
            analyses = listOf(
                analysis(entryId = "first", createdAt = 10L, summary = "old"),
                analysis(entryId = "first", createdAt = 50L, summary = "latest"),
                analysis(entryId = "second", createdAt = 40L, summary = "second"),
            ),
        )

        assertEquals(listOf("first", "second", "third"), items.map { it.meta.id })
        assertEquals("latest", items[0].latestAnalysis?.summary)
        assertEquals("second", items[1].latestAnalysis?.summary)
        assertNull(items[2].latestAnalysis)
    }

    private fun entry(id: String, updatedAt: Long): EntryMeta = EntryMeta(
        id = id,
        title = id,
        format = EntryFormat.MARKDOWN,
        filePath = "/tmp/$id.md",
        tags = emptyList(),
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    private fun analysis(entryId: String, createdAt: Long, summary: String): EmotionAnalysis = EmotionAnalysis(
        id = "$entryId-$createdAt",
        entryId = entryId,
        labels = listOf("平静"),
        intensity = 20,
        summary = summary,
        suggestions = emptyList(),
        safetyFlag = false,
        createdAt = createdAt,
    )
}
