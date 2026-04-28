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
    val bodyStressSignals: List<String> = emptyList(),
    val riskNotes: List<String> = emptyList(),
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


data class PsychologyAgentProcessEvent(
    val id: String,
    val runId: String,
    val entryId: String,
    val agentId: String,
    val agentName: String,
    val phase: PsychologyAgentPhase,
    val title: String,
    val contentMarkdown: String,
    val sequence: Int,
    val createdAt: Long,
    val isPartial: Boolean = false,
)

enum class PsychologyAgentPhase {
    ROUND_ONE,
    CRITIQUE,
    SYNTHESIS,
    PROFILE,
}

data class PsychologyAnalysisRun(
    val id: String,
    val entryId: String,
    val selectedAgentIds: List<String>,
    val status: PsychologyAnalysisRunStatus,
    val startedAt: Long,
    val completedAt: Long?,
    val finalAnalysisId: String?,
)

enum class PsychologyAnalysisRunStatus {
    RUNNING,
    COMPLETED,
    FAILED,
}

sealed interface PsychologyAgentRuntimeUpdate {
    data class Event(val event: PsychologyAgentProcessEvent) : PsychologyAgentRuntimeUpdate
    data class Completed(
        val analysis: EmotionAnalysis,
        val profile: UserPsychologyProfile,
        val runId: String,
    ) : PsychologyAgentRuntimeUpdate
}

data class UserPsychologyProfile(
    val triggers: List<String> = emptyList(),
    val cognitivePatterns: List<String> = emptyList(),
    val needs: List<String> = emptyList(),
    val relationshipPatterns: List<String> = emptyList(),
    val defensePatterns: List<String> = emptyList(),
    val bodyStressSignals: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val riskNotes: List<String> = emptyList(),
    val updatedAt: Long = 0L,
    val userEditedAt: Long = 0L,
)
