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
}
