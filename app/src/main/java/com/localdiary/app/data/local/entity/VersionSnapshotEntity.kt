package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "version_snapshots")
data class VersionSnapshotEntity(
    @PrimaryKey val versionId: String,
    val entryId: String,
    val source: String,
    val format: String,
    val filePath: String,
    val createdAt: Long,
)
