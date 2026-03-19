package com.localdiary.app.model

object EmotionPromptTemplate {
    fun validate(template: String): String? {
        if (template.isBlank()) return "情绪分析提示词模板不能为空。"
        val missingPlaceholders = AiEndpointConfig.REQUIRED_PLACEHOLDERS.filterNot(template::contains)
        if (missingPlaceholders.isNotEmpty()) {
            return "情绪分析提示词缺少变量：${missingPlaceholders.joinToString()}"
        }
        val missingOutputHints = AiEndpointConfig.REQUIRED_OUTPUT_HINTS.filterNot(template::contains)
        if (missingOutputHints.isNotEmpty()) {
            return "情绪分析提示词必须保留 JSON 输出字段约束。"
        }
        return null
    }

    fun render(
        template: String,
        entryText: String,
        format: EntryFormat,
        imageContext: String,
    ): String = template
        .replace(AiEndpointConfig.ENTRY_TEXT_PLACEHOLDER, entryText)
        .replace(AiEndpointConfig.ENTRY_FORMAT_PLACEHOLDER, format.label)
        .replace(
            AiEndpointConfig.IMAGE_CONTEXT_PLACEHOLDER,
            imageContext.ifBlank { "本次没有可供分析的图片内容。" },
        )
}
