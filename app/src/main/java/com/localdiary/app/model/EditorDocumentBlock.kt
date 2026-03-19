package com.localdiary.app.model

sealed interface EditorDocumentBlock {
    val id: String

    data class Text(
        override val id: String,
        val text: String,
    ) : EditorDocumentBlock

    data class Image(
        override val id: String,
        val rawSnippet: String,
        val dataUrl: String,
        val mimeType: String,
        val byteSize: Int,
    ) : EditorDocumentBlock
}
