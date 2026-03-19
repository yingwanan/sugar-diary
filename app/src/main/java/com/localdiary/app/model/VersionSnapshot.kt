package com.localdiary.app.model

data class VersionSnapshot(
    val versionId: String,
    val entryId: String,
    val source: String,
    val format: EntryFormat,
    val filePath: String,
    val createdAt: Long,
)
