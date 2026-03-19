package com.localdiary.app.model

enum class EntryFormat(val fileName: String, val extension: String) {
    MARKDOWN("content.md", "md"),
    HTML("content.html", "html");

    val label: String
        get() = extension.uppercase()

    val mimeType: String
        get() = when (this) {
            MARKDOWN -> "text/markdown"
            HTML -> "text/html"
        }

    companion object {
        fun fromFileName(name: String): EntryFormat? = when {
            name.endsWith(".md", ignoreCase = true) -> MARKDOWN
            name.endsWith(".html", ignoreCase = true) || name.endsWith(".htm", ignoreCase = true) -> HTML
            else -> null
        }
    }
}
