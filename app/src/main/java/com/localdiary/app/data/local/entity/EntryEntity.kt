package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val format: String,
    val filePath: String,
    val tagsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)
