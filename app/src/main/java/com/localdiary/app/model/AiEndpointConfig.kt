package com.localdiary.app.model

data class AiEndpointConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val requestTimeoutSeconds: Int = 90,
    val supportsVision: Boolean = false,
    val imageModelEnabled: Boolean = false,
    val imageBaseUrl: String = "",
    val imageApiKey: String = "",
    val imageModel: String = "",
    val emotionPromptTemplate: String = DEFAULT_EMOTION_PROMPT_TEMPLATE,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    val isImageUnderstandingConfigured: Boolean
        get() = imageModelEnabled && imageBaseUrl.isNotBlank() && imageApiKey.isNotBlank() && imageModel.isNotBlank()

    fun normalizedChatCompletionsUrl(): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        if (trimmed.endsWith("/chat/completions", ignoreCase = true)) return trimmed
        if (trimmed.endsWith("/v1", ignoreCase = true)) return "$trimmed/chat/completions"
        return "$trimmed/v1/chat/completions"
    }

    fun normalizedImageChatCompletionsUrl(): String {
        val trimmed = imageBaseUrl.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        if (trimmed.endsWith("/chat/completions", ignoreCase = true)) return trimmed
        if (trimmed.endsWith("/v1", ignoreCase = true)) return "$trimmed/chat/completions"
        return "$trimmed/v1/chat/completions"
    }

    companion object {
        const val ENTRY_TEXT_PLACEHOLDER = "{{entry_text}}"
        const val ENTRY_FORMAT_PLACEHOLDER = "{{entry_format}}"
        const val IMAGE_CONTEXT_PLACEHOLDER = "{{image_context}}"

        val REQUIRED_PLACEHOLDERS = listOf(
            ENTRY_TEXT_PLACEHOLDER,
            ENTRY_FORMAT_PLACEHOLDER,
            IMAGE_CONTEXT_PLACEHOLDER,
        )

        val REQUIRED_OUTPUT_HINTS = listOf(
            "\"labels\"",
            "\"intensity\"",
            "\"summary\"",
            "\"suggestions\"",
            "\"safetyFlag\"",
        )

        val DEFAULT_EMOTION_PROMPT_TEMPLATE = """
            你是情绪与心理状态整理助手。请结合正文与图片信息，识别使用者当前最突出的情绪、强度变化、触发诱因、认知线索与潜在压力来源，重点描述文字中可被支持的真实体验，不夸大、不贴诊断标签、不替代医疗判断。总结时保持克制、具体、尊重，优先输出可观察到的情绪模式与风险信号；建议以睡眠、社交、运动、减压、求助等日常健康策略为主，语气温和、可执行。文章格式{{entry_format}} 图片线索{{image_context}} 正文{{entry_text}} 返回JSON:{"labels":["..."],"intensity":0-100,"summary":"...","suggestions":["..."],"safetyFlag":false}
        """.trimIndent()
    }
}
