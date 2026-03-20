package com.localdiary.app.domain.browser

import com.localdiary.app.model.EmbeddedImageParser

object EntryPreviewFormatter {
    private val htmlTagRegex = Regex("""<[^>]+>""")
    private val markdownImageRegex = Regex("""!\[[^\]]*]\([^)]*\)""")
    private val markdownLinkRegex = Regex("""\[([^\]]+)]\([^)]*\)""")
    private val markdownDecorationRegex = Regex("""(^|\s)[>#*_`~\-]+""")
    private val htmlEntityRegex = Regex("""&(nbsp|amp|lt|gt|quot|#39);""")
    private val whitespaceRegex = Regex("""\s+""")
    private const val FALLBACK_PREVIEW = "暂无正文摘要"
    private const val MAX_PREVIEW_LENGTH = 140

    fun buildPreview(content: String): String {
        val sanitized = EmbeddedImageParser.sanitizeForLlm(content)
        var text = sanitized.content
        sanitized.placeholders.forEach { placeholder ->
            text = text.replace(placeholder, " ")
        }
        text = markdownImageRegex.replace(text, " ")
        text = markdownLinkRegex.replace(text, "$1")
        text = htmlTagRegex.replace(text, " ")
        text = markdownDecorationRegex.replace(text, " ")
        text = htmlEntityRegex.replace(text, " ")
        text = whitespaceRegex.replace(text, " ").trim()
        if (text.isBlank()) return FALLBACK_PREVIEW
        if (text.length <= MAX_PREVIEW_LENGTH) return text
        return text.take(MAX_PREVIEW_LENGTH).trimEnd() + "..."
    }
}
