package com.localdiary.app.domain.psychology

import com.localdiary.app.model.UserPsychologyProfile
import kotlinx.serialization.Serializable

@Serializable
data class UserPsychologyProfileUpdate(
    val triggers: List<String> = emptyList(),
    val cognitivePatterns: List<String> = emptyList(),
    val needs: List<String> = emptyList(),
    val relationshipPatterns: List<String> = emptyList(),
    val defensePatterns: List<String> = emptyList(),
    val bodyStressSignals: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val riskNotes: List<String> = emptyList(),
)

object UserPsychologyProfileMerger {
    fun merge(
        current: UserPsychologyProfile,
        update: UserPsychologyProfileUpdate,
        now: Long,
    ): UserPsychologyProfile = current.copy(
        triggers = mergeList(current.triggers, update.triggers),
        cognitivePatterns = mergeList(current.cognitivePatterns, update.cognitivePatterns),
        needs = mergeList(current.needs, update.needs),
        relationshipPatterns = mergeList(current.relationshipPatterns, update.relationshipPatterns),
        defensePatterns = mergeList(current.defensePatterns, update.defensePatterns),
        bodyStressSignals = mergeList(current.bodyStressSignals, update.bodyStressSignals),
        strengths = mergeList(current.strengths, update.strengths),
        riskNotes = mergeList(current.riskNotes, update.riskNotes),
        updatedAt = now,
    )

    private fun mergeList(existing: List<String>, incoming: List<String>, maxItems: Int = 12): List<String> {
        val seen = linkedSetOf<String>()
        (existing + incoming)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { item ->
                val normalized = item.lowercase()
                if (seen.none { it.lowercase() == normalized }) seen.add(item)
            }
        return seen.take(maxItems)
    }
}
