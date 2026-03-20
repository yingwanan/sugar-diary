package com.localdiary.app.domain.search

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

object EntrySearchMatcher {
    private val yearPattern = Regex("""^\d{4}$""")
    private val yearMonthPattern = Regex("""^\d{4}-\d{2}$""")
    private val dayPattern = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun matches(
        title: String,
        query: String,
        timestamps: List<Long>,
    ): Boolean {
        if (query.isBlank()) return true
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        if (title.lowercase(Locale.getDefault()).contains(normalizedQuery)) {
            return true
        }
        val dateQuery = parseDateQuery(normalizedQuery) ?: return false
        return timestamps.any { timestamp ->
            val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            dateQuery.matches(date)
        }
    }

    private fun parseDateQuery(query: String): DateQuery? = when {
        dayPattern.matches(query) -> runCatching { DateQuery.Day(LocalDate.parse(query)) }.getOrNull()
        yearMonthPattern.matches(query) -> runCatching { DateQuery.Month(YearMonth.parse(query)) }.getOrNull()
        yearPattern.matches(query) -> query.toIntOrNull()?.let(DateQuery::Year)
        else -> null
    }

    private sealed interface DateQuery {
        fun matches(date: LocalDate): Boolean

        data class Year(val year: Int) : DateQuery {
            override fun matches(date: LocalDate): Boolean = date.year == year
        }

        data class Month(val yearMonth: YearMonth) : DateQuery {
            override fun matches(date: LocalDate): Boolean = YearMonth.from(date) == yearMonth
        }

        data class Day(val dateValue: LocalDate) : DateQuery {
            override fun matches(date: LocalDate): Boolean = date == dateValue
        }
    }
}
