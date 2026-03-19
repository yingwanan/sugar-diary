package com.localdiary.app.domain.browser

import com.localdiary.app.model.BrowserTimeBucket
import com.localdiary.app.model.EntryBrowserItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object EntryBrowserFilter {
    fun filter(
        items: List<EntryBrowserItem>,
        titleQuery: String,
        dateQuery: String,
        selectedTag: String?,
        selectedMood: String?,
        selectedTimeBucket: BrowserTimeBucket?,
        now: LocalDate = LocalDate.now(),
    ): List<EntryBrowserItem> {
        return items.filter { item ->
            matchesTitle(item, titleQuery) &&
                matchesDate(item, dateQuery) &&
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

    private fun matchesTitle(item: EntryBrowserItem, titleQuery: String): Boolean {
        if (titleQuery.isBlank()) return true
        return item.meta.title.lowercase(Locale.getDefault())
            .contains(titleQuery.trim().lowercase(Locale.getDefault()))
    }

    private fun matchesDate(item: EntryBrowserItem, dateQuery: String): Boolean {
        if (dateQuery.isBlank()) return true
        val query = dateQuery.trim()
        val created = searchableDates(item.meta.createdAt)
        val updated = searchableDates(item.meta.updatedAt)
        return (created + updated).any { it.contains(query) }
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

    private fun searchableDates(timestamp: Long): List<String> {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return listOf(
            date.toString(),
            date.withDayOfMonth(1).toString().substring(0, 7),
        )
    }
}
