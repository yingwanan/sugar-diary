package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "psychology_chat_messages")
data class PsychologyChatMessageEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)
