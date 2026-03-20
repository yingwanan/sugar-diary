package com.localdiary.app.model

data class EmotionCenterItem(
    val meta: EntryMeta,
    val latestAnalysis: EmotionAnalysis? = null,
)
