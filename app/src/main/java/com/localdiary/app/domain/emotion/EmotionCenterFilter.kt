package com.localdiary.app.domain.emotion

import com.localdiary.app.domain.search.EntrySearchMatcher
import com.localdiary.app.model.EmotionCenterItem

object EmotionCenterFilter {
    fun filter(
        items: List<EmotionCenterItem>,
        query: String,
    ): List<EmotionCenterItem> {
        if (query.isBlank()) return items
        return items.filter { item ->
            EntrySearchMatcher.matches(
                title = item.meta.title,
                query = query,
                timestamps = listOfNotNull(
                    item.latestAnalysis?.createdAt,
                    item.meta.updatedAt,
                    item.meta.createdAt,
                ).distinct(),
            )
        }
    }
}
