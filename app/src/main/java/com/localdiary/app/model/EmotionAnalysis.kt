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
    val triggers: List<String> = emptyList(),
    val cognitivePatterns: List<String> = emptyList(),
    val needs: List<String> = emptyList(),
    val relationshipSignals: List<String> = emptyList(),
    val defenseMechanisms: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val bodyStressSignals: List<String> = emptyList(),
    val riskNotes: List<String> = emptyList(),
)
