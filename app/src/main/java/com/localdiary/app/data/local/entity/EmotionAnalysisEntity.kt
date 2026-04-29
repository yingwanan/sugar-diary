package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "emotion_analyses")
data class EmotionAnalysisEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val labelsJson: String,
    val intensity: Int,
    val summary: String,
    val suggestionsJson: String,
    val safetyFlag: Boolean,
    val createdAt: Long,
    val triggersJson: String = "[]",
    val cognitivePatternsJson: String = "[]",
    val needsJson: String = "[]",
    val relationshipSignalsJson: String = "[]",
    val defenseMechanismsJson: String = "[]",
    val strengthsJson: String = "[]",
    val bodyStressSignalsJson: String = "[]",
    val riskNotesJson: String = "[]",
)
