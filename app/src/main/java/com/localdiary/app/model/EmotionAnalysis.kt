package com.localdiary.app.model

data class EmotionAnalysis(
    val id: String,
    val entryId: String,
    val labels: List<String>,
    val intensity: Int,
    val summary: String,
    val suggestions: List<String>,
    val safetyFlag: Boolean,
    val createdAt: Long,
)
