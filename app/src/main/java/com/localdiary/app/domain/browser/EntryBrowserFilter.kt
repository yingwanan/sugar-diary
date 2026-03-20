package com.localdiary.app.domain.browser

import com.localdiary.app.model.BrowserTimeBucket
import com.localdiary.app.model.EntryBrowserItem
import com.localdiary.app.domain.search.EntrySearchMatcher
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object EntryBrowserFilter {
    fun filter(
        items: List<EntryBrowserItem>,
        query: String,
        selectedTag: String?,
        selectedMood: String?,
        selectedTimeBucket: BrowserTimeBucket?,
        now: LocalDate = LocalDate.now(),
    ): List<EntryBrowserItem> {
        return items.filter { item ->
            matchesSearch(item, query) &&
                matchesTag(item, selectedTag) &&
                matchesMood(item, selectedMood) &&
                matchesTimeBucket(item, selectedTimeBucket, now)
        }
    }

    fun groupTimeBucket(timestamp: Long, now: LocalDate = LocalDate.now()): BrowserTimeBucket {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val weekStart = now.with(java.time.DayOfWeek.MONDAY)
        val monthStart = now.with(TemporalAdjusters.firstDayOfMonth())
        return when {
            date == now -> BrowserTimeBucket.TODAY
            !date.isBefore(weekStart) -> BrowserTimeBucket.THIS_WEEK
            !date.isBefore(monthStart) -> BrowserTimeBucket.THIS_MONTH
            else -> BrowserTimeBucket.EARLIER
        }
    }

    private fun matchesSearch(item: EntryBrowserItem, query: String): Boolean {
        return EntrySearchMatcher.matches(
            title = item.meta.title,
            query = query,
            timestamps = listOf(item.meta.createdAt, item.meta.updatedAt),
        )
    }

    private fun matchesTag(item: EntryBrowserItem, selectedTag: String?): Boolean {
        if (selectedTag.isNullOrBlank()) return true
        return item.meta.tags.any { it == selectedTag }
    }

    private fun matchesMood(item: EntryBrowserItem, selectedMood: String?): Boolean {
        if (selectedMood.isNullOrBlank()) return true
        return item.latestEmotion?.labels?.any { it == selectedMood } == true
    }

    private fun matchesTimeBucket(
        item: EntryBrowserItem,
        selectedTimeBucket: BrowserTimeBucket?,
        now: LocalDate,
    ): Boolean {
        if (selectedTimeBucket == null) return true
        return groupTimeBucket(item.meta.updatedAt, now) == selectedTimeBucket
    }
}
