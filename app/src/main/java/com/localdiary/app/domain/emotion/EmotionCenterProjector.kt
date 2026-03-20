package com.localdiary.app.domain.emotion

import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.EmotionCenterItem
import com.localdiary.app.model.EntryMeta

object EmotionCenterProjector {
    fun project(
        entries: List<EntryMeta>,
        analyses: List<EmotionAnalysis>,
    ): List<EmotionCenterItem> {
        val latestByEntry = analyses
            .groupBy { it.entryId }
            .mapValues { (_, items) -> items.maxByOrNull { it.createdAt } }

        return entries
            .map { entry ->
                EmotionCenterItem(
                    meta = entry,
                    latestAnalysis = latestByEntry[entry.id],
                )
            }
            .sortedWith(
                compareByDescending<EmotionCenterItem> { it.latestAnalysis?.createdAt ?: Long.MIN_VALUE }
                    .thenByDescending { it.meta.updatedAt },
            )
    }
}
