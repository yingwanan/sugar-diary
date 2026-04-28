package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "psychology_agent_process_events")
data class PsychologyAgentProcessEventEntity(
    @PrimaryKey val id: String,
    val runId: String,
    val entryId: String,
    val agentId: String,
    val agentName: String,
    val phase: String,
    val title: String,
    val contentMarkdown: String,
    val sequence: Int,
    val createdAt: Long,
)
