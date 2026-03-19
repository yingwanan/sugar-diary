package com.localdiary.app.data.image

data class EmbeddedImageInsertResult(
    val rawSnippet: String,
    val dataUrl: String,
    val mimeType: String,
    val byteSize: Int,
)
