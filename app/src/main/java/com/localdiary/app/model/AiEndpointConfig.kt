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
            "\"triggers\"",
            "\"cognitivePatterns\"",
            "\"needs\"",
            "\"relationshipSignals\"",
            "\"defenseMechanisms\"",
            "\"strengths\"",
        )

        val DEFAULT_EMOTION_PROMPT_TEMPLATE = """
            你现在是一个专业心理分析 agent 的核心分析模块。你的任务不是只做心理识别，而是基于用户日记正文与图片线索，生成完整、温和、非评判、可行动的心理洞察。

            重要边界：你不能替代心理咨询、医学诊断或危机干预；不要给出疾病诊断、处方、确定性病理判断或治疗承诺。你只能提供心理观察、自我理解线索和支持性改善建议。

            请严格输出 JSON，对象字段只能包含 "labels"、"intensity"、"summary"、"suggestions"、"safetyFlag"、"triggers"、"cognitivePatterns"、"needs"、"relationshipSignals"、"defenseMechanisms"、"strengths"：
            1. "labels"：1 到 4 个高颗粒度心理或状态标签，可包含强度，例如 "焦虑(8/10)"、"压抑(6/10)"。
            2. "intensity"：整体心理/心理波动强度，0 到 100。
            3. "triggers"：具体触发事件、场景或线索，避免泛泛而谈。
            4. "cognitivePatterns"：可能出现的认知模式，例如读心推测、灾难化、过度负责、全或无思维；只能用“可能/倾向”表达。
            5. "needs"：未被满足或正在被表达的深层心理需求，例如安全感、被理解、边界、掌控感、休息。
            6. "relationshipSignals"：关系互动、边界、依恋、支持系统或压力来源线索；没有就返回空数组。
            7. "defenseMechanisms"：可能的防御或应对方式，例如回避、合理化、压抑、讨好、投射；避免诊断化。
            8. "strengths"：用户呈现出的心理资源、韧性、价值感或积极能力。
            9. "summary"：整合成一段专业心理画像，覆盖触发机制、认知/需求/关系线索、防御或应对方式、资源优势。
            10. "suggestions"：3 到 6 条具体、可执行、低负担的改善建议，优先包括记录触发点、需求命名、边界练习、身体稳定、现实支持等。
            11. "safetyFlag"：仅在出现明显自伤、自杀、伤人、极端绝望或现实安全高风险信号时返回 true，否则 false。

            文章格式{{entry_format}}
            图片线索{{image_context}}
            正文{{entry_text}}

            返回JSON:{"labels":["焦虑(8/10)"],"intensity":78,"summary":"...","suggestions":["..."],"safetyFlag":false,"triggers":["..."],"cognitivePatterns":["..."],"needs":["..."],"relationshipSignals":["..."],"defenseMechanisms":["..."],"strengths":["..."]}
        """.trimIndent()
    }
}
