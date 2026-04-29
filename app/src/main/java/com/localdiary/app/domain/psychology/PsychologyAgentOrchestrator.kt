package com.localdiary.app.domain.psychology

import com.localdiary.app.data.llm.LlmProvider
import com.localdiary.app.model.AiEndpointConfig
import com.localdiary.app.model.EntryDocument
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.PsychologyAgentPhase
import com.localdiary.app.model.PsychologyAgentProcessEvent
import com.localdiary.app.model.PsychologyAgentRuntimeUpdate
import com.localdiary.app.model.PsychologyAnalysisResult
import com.localdiary.app.model.UserPsychologyProfile
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json

class PsychologyAgentOrchestrator(
    private val llmProvider: LlmProvider,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun run(
        config: AiEndpointConfig,
        document: EntryDocument,
        sanitizedContent: String,
        format: EntryFormat,
        latestAnalysis: EmotionAnalysis?,
        historicalAnalyses: List<EmotionAnalysis>,
        currentProfile: UserPsychologyProfile,
        selectedAgentId: String?,
        runId: String = UUID.randomUUID().toString(),
    ): Flow<PsychologyAgentRuntimeUpdate> = channelFlow {
        val selectedAgents = PsychologyAgentCatalog.resolveSelection(selectedAgentId)
        val sequence = AtomicInteger(0)
        val context = PsychologyAgentContextBuilder.build(
            currentTitle = document.meta.title,
            currentContent = sanitizedContent,
            latestAnalysis = latestAnalysis,
            historicalAnalyses = historicalAnalyses,
        )
        val profileBlock = currentProfile.toPromptBlock()
        val baseBoundary = PsychologyAgentContextBuilder.chatSystemPrompt("$context\n\n用户画像：\n$profileBlock")

        fun event(
            agent: PsychologyAgentDefinition,
            phase: PsychologyAgentPhase,
            title: String,
            content: String,
            partial: Boolean,
        ) = PsychologyAgentRuntimeUpdate.Event(
            PsychologyAgentProcessEvent(
                id = UUID.randomUUID().toString(),
                runId = runId,
                entryId = document.meta.id,
                agentId = agent.id,
                agentName = agent.displayName,
                phase = phase,
                title = title,
                contentMarkdown = content,
                sequence = sequence.incrementAndGet(),
                createdAt = System.currentTimeMillis(),
                isPartial = partial,
            ),
        )

        suspend fun runAgent(
            agent: PsychologyAgentDefinition,
            phase: PsychologyAgentPhase,
            title: String,
            extraContext: String,
        ): String {
            val prompt = """
                $baseBoundary

                你现在扮演：${agent.displayName}
                专项职责：${agent.focus}
                执行要求：${agent.prompt}
                输出 Markdown，保持专业、具体、非诊断，最多 6 条要点。
                $extraContext
            """.trimIndent()
            val userPrompt = "请完成 ${agent.displayName} 的${if (phase == PsychologyAgentPhase.CRITIQUE) "补充/反驳" else "专项分析"}。"
            val builder = StringBuilder()
            val streamSucceeded = runCatching {
                llmProvider.streamPsychologyText(config, prompt, userPrompt).collect { token ->
                    builder.append(token)
                    trySend(event(agent, phase, title, builder.toString(), partial = true))
                }
            }.isSuccess
            if (!streamSucceeded || builder.isBlank()) {
                builder.clear()
                builder.append(llmProvider.completePsychologyText(config, prompt, userPrompt))
                trySend(event(agent, phase, title, builder.toString(), partial = true))
            }
            val text = builder.toString().trim()
            trySend(event(agent, phase, title, text, partial = false))
            return text
        }

        suspend fun runSpecialAgent(
            agent: PsychologyAgentDefinition,
            phase: PsychologyAgentPhase,
            title: String,
            systemPrompt: String,
            userPrompt: String,
        ): String {
            val builder = StringBuilder()
            val streamSucceeded = runCatching {
                llmProvider.streamPsychologyText(config, systemPrompt, userPrompt).collect { token ->
                    builder.append(token)
                    trySend(event(agent, phase, title, builder.toString(), partial = true))
                }
            }.isSuccess
            if (!streamSucceeded || builder.isBlank()) {
                builder.clear()
                builder.append(llmProvider.completePsychologyText(config, systemPrompt, userPrompt))
                trySend(event(agent, phase, title, builder.toString(), partial = true))
            }
            val text = builder.toString().trim()
            trySend(event(agent, phase, title, text, partial = false))
            return text
        }

        val roundOne = coroutineScope {
            selectedAgents.map { agent ->
                async {
                    agent to runAgent(agent, PsychologyAgentPhase.ROUND_ONE, "专项分析", "")
                }
            }.awaitAll()
        }

        val critiqueInput = roundOne.joinToString("\n\n") { (agent, text) -> "## ${agent.displayName}\n$text" }
        val roundTwo = if (selectedAgents.size > 1) {
            coroutineScope {
                selectedAgents.map { agent ->
                    async {
                        agent to runAgent(
                            agent = agent,
                            phase = PsychologyAgentPhase.CRITIQUE,
                            title = "补充 / 反驳",
                            extraContext = "其他 Agent 初步观点如下。请只补充你认为缺失、过度推断或需要修正之处：\n$critiqueInput",
                        )
                    }
                }.awaitAll()
            }
        } else {
            emptyList()
        }

        val synthesisAgent = PsychologyAgentDefinition(
            id = "synthesizer",
            displayName = if (selectedAgents.size == 1) "专项综合 Agent" else "综合 Agent",
            focus = "整合专项分析、反驳意见和用户画像，生成最终心理分析。",
            prompt = "输出必须是严格 JSON，字段兼容 PsychologyAnalysisResult。summary 和 suggestions 可以包含少量 Markdown。",
        )
        val synthesisPrompt = """
            $baseBoundary

            你现在扮演综合 Agent。请整合专项 Agent 分析、补充/反驳和用户画像，生成最终分析。
            必须严格输出 JSON，对象字段为：labels, intensity, summary, suggestions, safetyFlag, triggers, cognitivePatterns, needs, relationshipSignals, defenseMechanisms, strengths, bodyStressSignals, riskNotes。
            不要输出 JSON 以外的文字。

            专项分析：
            ${roundOne.joinToString("\n\n") { (agent, text) -> "## ${agent.displayName}\n$text" }}

            补充/反驳：
            ${roundTwo.joinToString("\n\n") { (agent, text) -> "## ${agent.displayName}\n$text" }.ifBlank { "单 Agent 专项模式，无跨 Agent 反驳。" }}
        """.trimIndent()
        val synthesisText = runSpecialAgent(
            agent = synthesisAgent,
            phase = PsychologyAgentPhase.SYNTHESIS,
            title = "最终综合",
            systemPrompt = synthesisPrompt,
            userPrompt = "请生成最终心理分析 JSON。",
        )
        val analysisResult = PsychologyJsonExtractor.decodeAnalysisResultOrThrow(synthesisText, "最终心理分析")

        val profileAgent = PsychologyAgentDefinition(
            id = "profile_updater",
            displayName = "画像更新 Agent",
            focus = "从本次分析中提取可长期复用、非诊断的用户画像线索。",
            prompt = "输出必须是严格 JSON，字段兼容 UserPsychologyProfileUpdate。",
        )
        val profilePrompt = """
            $baseBoundary

            请根据本次最终分析，提取适合长期保存的用户画像更新。只保留反复可能有用、非诊断、可被用户修正的线索。
            必须严格输出 JSON，字段为 triggers, cognitivePatterns, needs, relationshipPatterns, defensePatterns, bodyStressSignals, strengths, riskNotes。
            最终分析 JSON：
            $synthesisText
        """.trimIndent()
        val profileUpdateText = runSpecialAgent(
            agent = profileAgent,
            phase = PsychologyAgentPhase.PROFILE,
            title = "画像更新",
            systemPrompt = profilePrompt,
            userPrompt = "请生成用户画像更新 JSON。",
        )
        val profileUpdate = decodeJsonOrError<UserPsychologyProfileUpdate>(profileUpdateText, "用户画像更新")
        send(PsychologyAgentRuntimeUpdate.Event(
            PsychologyAgentProcessEvent(
                id = UUID.randomUUID().toString(),
                runId = runId,
                entryId = document.meta.id,
                agentId = "runtime_result",
                agentName = "运行结果",
                phase = PsychologyAgentPhase.SYNTHESIS,
                title = "__RESULT__",
                contentMarkdown = json.encodeToString(PsychologyAnalysisResult.serializer(), analysisResult) + "\n---PROFILE---\n" + json.encodeToString(UserPsychologyProfileUpdate.serializer(), profileUpdate),
                sequence = sequence.incrementAndGet(),
                createdAt = System.currentTimeMillis(),
                isPartial = false,
            ),
        ))
    }

    private inline fun <reified T> decodeJsonOrError(raw: String, label: String): T =
        PsychologyJsonExtractor.decodeOrThrow(raw, label)
}

private fun UserPsychologyProfile.toPromptBlock(): String = buildString {
    appendList("长期触发点", triggers)
    appendList("认知倾向", cognitivePatterns)
    appendList("核心需求", needs)
    appendList("关系模式", relationshipPatterns)
    appendList("防御/应对", defensePatterns)
    appendList("身体压力", bodyStressSignals)
    appendList("资源优势", strengths)
    appendList("风险注意", riskNotes)
    if (isBlank()) append("暂无已保存画像。")
}

private fun StringBuilder.appendList(label: String, values: List<String>) {
    if (values.isNotEmpty()) appendLine("$label: ${values.joinToString("；")}")
}
