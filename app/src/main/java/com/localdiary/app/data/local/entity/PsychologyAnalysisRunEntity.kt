package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "psychology_analysis_runs")
data class PsychologyAnalysisRunEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val selectedAgentIdsJson: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
    val finalAnalysisId: String?,
)
