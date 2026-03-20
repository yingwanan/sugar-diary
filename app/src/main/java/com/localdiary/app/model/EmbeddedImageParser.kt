package com.localdiary.app.model

object EmbeddedImageParser {
    private val dataUrlRegex = Regex(
        pattern = """data:image/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=\r\n]+""",
    )

    fun extractDataUrls(content: String, limit: Int = 4): List<String> =
        dataUrlRegex.findAll(content)
            .map { it.value.replace("\n", "").replace("\r", "") }
            .distinct()
            .take(limit)
            .toList()

    fun sanitizeForLlm(content: String): SanitizedEmbeddedContent {
        val replacements = linkedMapOf<String, String>()
        var nextIndex = 1
        val sanitized = dataUrlRegex.replace(content) { match ->
            val normalized = match.value.replace("\n", "").replace("\r", "")
            replacements.entries.firstOrNull { it.value == normalized }?.key ?: run {
                val placeholder = "[[EMBEDDED_IMAGE_${nextIndex++}]]"
                replacements[placeholder] = normalized
                placeholder
            }
        }
        return SanitizedEmbeddedContent(
            content = sanitized,
            imageDataUrls = replacements.values.toList(),
            placeholders = replacements.keys.toList(),
            replacementMap = replacements.toMap(),
        )
    }

    fun restorePlaceholders(
        content: String,
        sanitizedEmbeddedContent: SanitizedEmbeddedContent,
    ): String {
        var restored = content
        sanitizedEmbeddedContent.replacementMap.forEach { (placeholder, dataUrl) ->
            restored = restored.replace(placeholder, dataUrl)
        }
        return restored
    }

    data class SanitizedEmbeddedContent(
        val content: String,
        val imageDataUrls: List<String>,
        val placeholders: List<String>,
        internal val replacementMap: Map<String, String>,
    )
}
