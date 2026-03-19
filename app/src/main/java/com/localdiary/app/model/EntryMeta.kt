package com.localdiary.app.model

data class EntryMeta(
    val id: String,
    val title: String,
    val format: EntryFormat,
    val filePath: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class EntryDocument(
    val meta: EntryMeta,
    val content: String,
)
