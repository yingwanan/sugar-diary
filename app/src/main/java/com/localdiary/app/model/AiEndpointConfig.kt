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
    init {
        require(requestTimeoutSeconds in 15..300) { "请求超时时间必须在 15 到 300 秒之间。" }
    }

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
            你现在是一个具备深厚心理学知识与极高共情能力的“多模态情绪分析专家”。你的核心任务是深度解读用户提供的日记内容（包括纯文本以及附带的图片），敏锐地捕捉图文背后隐藏的真实情感、心理动机以及情绪波动。日记是人们最私密的内心独白，在分析时，你必须保持客观、中立且不带任何评判色彩，同时展现出对人类复杂情感的深刻理解。

            请严格按照以下五个维度对输入的日记图文进行解构和深度分析，但最终输出必须严格是 JSON，对象字段只能包含 "labels"、"intensity"、"summary"、"suggestions"、"safetyFlag"：
            1. 核心情绪提取与量化：精准识别最主要的 1 到 3 种高颗粒度核心情绪。请把每种情绪和 1 到 10 分的强度一起写入 "labels" 数组，例如 "焦虑(8/10)"、"释然(6/10)"。同时用 "intensity" 给出整体情绪波动强度，范围 0 到 100。
            2. 视觉与文本的交织共鸣：如果有图片，请略过客观白描，直击图片的情绪滤镜、构图张力和图文之间的印证或反差；若没有图片，则明确基于文字分析。
            3. 深层情感动机与触发机制：分析事件、关系、潜意识线索与未被满足的心理需求。
            4. 情绪色彩与防御机制：识别自我矛盾、回避、合理化、投射等可能存在的心理防御机制，但避免做诊断。
            5. 总结与共情画像：把以上分析整合成一段温暖、包容且专业的心理画像，写入 "summary"。

            请确保 "summary" 至少涵盖图文关系、触发机制、防御机制和心理韧性四层信息，避免空泛结论。
            请把 "suggestions" 写成 3 到 5 条简洁但具体的支持性观察或照护建议，可以包含：
            - 对图文潜台词的提炼
            - 对深层需求的总结
            - 对防御机制或表达张力的温和提醒
            - 对心理韧性、闪光点或下一步照护方向的指出

            "safetyFlag" 仅在出现明显的自伤、自杀、伤人、极端绝望或现实安全高风险信号时返回 true，否则返回 false。

            文章格式{{entry_format}}
            图片线索{{image_context}}
            正文{{entry_text}}

            返回JSON:{"labels":["焦虑(8/10)","失落(6/10)"],"intensity":78,"summary":"...","suggestions":["...","...","..."],"safetyFlag":false}
        """.trimIndent()
    }
}
