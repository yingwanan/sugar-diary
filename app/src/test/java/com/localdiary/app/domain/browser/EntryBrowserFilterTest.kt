package com.localdiary.app.domain.browser

import com.localdiary.app.model.BrowserTimeBucket
import com.localdiary.app.model.EntryBrowserItem
import com.localdiary.app.model.EntryEmotionSummary
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.EntryMeta
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryBrowserFilterTest {
    private val zone = ZoneId.systemDefault()
    private val now = LocalDate.of(2026, 3, 20)

    private val items = listOf(
        EntryBrowserItem(
            meta = EntryMeta(
                id = "a",
                title = "春天记录",
                format = EntryFormat.MARKDOWN,
                filePath = "/tmp/a",
                tags = listOf("生活", "散步"),
                createdAt = timestampOf(2026, 3, 20),
                updatedAt = timestampOf(2026, 3, 20),
            ),
            latestEmotion = EntryEmotionSummary(
                labels = listOf("平静", "轻松"),
                summary = "状态稳定",
                createdAt = timestampOf(2026, 3, 20),
            ),
        ),
        EntryBrowserItem(
            meta = EntryMeta(
                id = "b",
                title = "工作复盘",
                format = EntryFormat.HTML,
                filePath = "/tmp/b",
                tags = listOf("工作"),
                createdAt = timestampOf(2026, 3, 10),
                updatedAt = timestampOf(2026, 3, 18),
            ),
            latestEmotion = EntryEmotionSummary(
                labels = listOf("焦虑"),
                summary = "压力偏高",
                createdAt = timestampOf(2026, 3, 18),
            ),
        ),
        EntryBrowserItem(
            meta = EntryMeta(
                id = "c",
                title = "二月总结",
                format = EntryFormat.MARKDOWN,
                filePath = "/tmp/c",
                tags = listOf("总结"),
                createdAt = timestampOf(2026, 2, 25),
                updatedAt = timestampOf(2026, 2, 25),
            ),
        ),
    )

    @Test
    fun `filter matches title query`() {
        val result = EntryBrowserFilter.filter(
            items = items,
            query = "工作",
            selectedTag = null,
            selectedMood = null,
            selectedTimeBucket = null,
            now = now,
        )

        assertEquals(listOf("b"), result.map { it.meta.id })
    }

    @Test
    fun `filter matches date query by month`() {
        val result = EntryBrowserFilter.filter(
            items = items,
            query = "2026-02",
            selectedTag = null,
            selectedMood = null,
            selectedTimeBucket = null,
            now = now,
        )

        assertEquals(listOf("c"), result.map { it.meta.id })
    }

    @Test
    fun `filter matches selected mood from latest analysis`() {
        val result = EntryBrowserFilter.filter(
            items = items,
            query = "",
            selectedTag = null,
            selectedMood = "平静",
            selectedTimeBucket = null,
            now = now,
        )

        assertEquals(listOf("a"), result.map { it.meta.id })
    }

    @Test
    fun `filter matches combined title and date search`() {
        val titleResult = EntryBrowserFilter.filter(
            items = items,
            query = "春天",
            selectedTag = null,
            selectedMood = null,
            selectedTimeBucket = null,
            now = now,
        )
        val dateResult = EntryBrowserFilter.filter(
            items = items,
            query = "2026-03-10",
            selectedTag = null,
            selectedMood = null,
            selectedTimeBucket = null,
            now = now,
        )

        assertEquals(listOf("a"), titleResult.map { it.meta.id })
        assertEquals(listOf("b"), dateResult.map { it.meta.id })
    }

    @Test
    fun `filter matches exact day without leaking other dates`() {
        val result = EntryBrowserFilter.filter(
            items = items,
            query = "2026-03-1",
            selectedTag = null,
            selectedMood = null,
            selectedTimeBucket = null,
            now = now,
        )

        assertEquals(emptyList<String>(), result.map { it.meta.id })
    }

    @Test
    fun `filter matches year query`() {
        val result = EntryBrowserFilter.filter(
            items = items,
            query = "2026",
            selectedTag = null,
            selectedMood = null,
            selectedTimeBucket = null,
            now = now,
        )

        assertEquals(listOf("a", "b", "c"), result.map { it.meta.id })
    }

    @Test
    fun `groupTimeBucket distinguishes today week month and earlier`() {
        assertEquals(BrowserTimeBucket.TODAY, EntryBrowserFilter.groupTimeBucket(timestampOf(2026, 3, 20), now))
        assertEquals(BrowserTimeBucket.THIS_WEEK, EntryBrowserFilter.groupTimeBucket(timestampOf(2026, 3, 18), now))
        assertEquals(BrowserTimeBucket.EARLIER, EntryBrowserFilter.groupTimeBucket(timestampOf(2026, 2, 25), now))
    }

    private fun timestampOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()
}
