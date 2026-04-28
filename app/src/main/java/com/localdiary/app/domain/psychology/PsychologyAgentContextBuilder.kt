package com.localdiary.app.domain.psychology

import com.localdiary.app.model.EmotionAnalysis

object PsychologyAgentContextBuilder {
    fun build(
        currentTitle: String,
        currentContent: String,
        latestAnalysis: EmotionAnalysis?,
        historicalAnalyses: List<EmotionAnalysis>,
        maxHistoryItems: Int = 6,
    ): String = buildString {
        appendLine("当前日记标题: ${currentTitle.ifBlank { "未命名记录" }}")
        appendLine("当前日记正文:")
        appendLine(currentContent.ifBlank { "（当前日记正文为空。）" })
        if (latestAnalysis != null) {
            appendLine()
            appendLine("最近心理分析:")
            appendAnalysis(latestAnalysis)
        }
        val boundedHistory = historicalAnalyses
            .filterNot { latestAnalysis != null && it.id == latestAnalysis.id }
            .take(maxHistoryItems.coerceAtLeast(0))
        if (boundedHistory.isNotEmpty()) {
            appendLine()
            appendLine("历史心理线索:")
            boundedHistory.forEachIndexed { index, analysis ->
                appendLine("${index + 1}.")
                appendAnalysis(analysis)
            }
        }
    }.trim()

    fun chatSystemPrompt(context: String): String = """
        你是运行在用户手机本地编排层中的专业心理分析 agent，远程模型只负责生成回复。
        你基于用户主动提供的日记、历史心理分析摘要和当前问题，提供温和、具体、非评判的心理洞察。
        你可以分析心理、触发点、认知模式、深层需求、关系线索、防御机制、资源优势，并给出可执行的自我照护建议。
        你不能替代心理咨询、医学诊断或危机干预，不要给出疾病诊断、处方或确定性病理判断。
        如果用户内容出现自伤、自杀、伤人或现实安全风险，只能温和提醒用户尽快联系现实支持或当地紧急/专业资源。

        本地上下文如下：
        $context
    """.trimIndent()

    private fun StringBuilder.appendAnalysis(analysis: EmotionAnalysis) {
        appendLine("标签: ${analysis.labels.joinToString().ifBlank { "无" }}")
        appendLine("强度: ${analysis.intensity}/100")
        appendLine("摘要: ${analysis.summary}")
        appendList("触发点", analysis.triggers)
        appendList("认知模式", analysis.cognitivePatterns)
        appendList("深层需求", analysis.needs)
        appendList("关系线索", analysis.relationshipSignals)
        appendList("防御机制", analysis.defenseMechanisms)
        appendList("资源优势", analysis.strengths)
        appendList("建议", analysis.suggestions)
    }

    private fun StringBuilder.appendList(label: String, values: List<String>) {
        if (values.isNotEmpty()) appendLine("$label: ${values.joinToString("；")}")
    }
}
