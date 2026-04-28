package com.localdiary.app.model

import kotlinx.serialization.Serializable

data class ReviewResult(
    val issues: List<String>,
    val suggestedTitle: String?,
    val candidateContent: String,
    val format: EntryFormat,
)

data class PolishCandidate(
    val styleName: String,
    val rationale: String,
    val content: String,
    val format: EntryFormat,
)

@Serializable
data class PsychologyAnalysisResult(
    val labels: List<String>,
    val intensity: Int,
    val summary: String,
    val suggestions: List<String>,
    val safetyFlag: Boolean,
    val triggers: List<String> = emptyList(),
    val cognitivePatterns: List<String> = emptyList(),
    val needs: List<String> = emptyList(),
    val relationshipSignals: List<String> = emptyList(),
    val defenseMechanisms: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
)

@Serializable
data class PeriodicReportResult(
    val dominantMoods: List<String>,
    val summary: String,
    val advice: List<String>,
)

data class PsychologyChatMessage(
    val id: String,
    val entryId: String,
    val role: PsychologyChatRole,
    val content: String,
    val createdAt: Long,
)

enum class PsychologyChatRole {
    USER,
    ASSISTANT,
}

@Serializable
data class PsychologyChatResult(
    val reply: String,
)
