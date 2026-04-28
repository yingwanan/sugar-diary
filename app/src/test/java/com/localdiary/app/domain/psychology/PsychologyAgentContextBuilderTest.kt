package com.localdiary.app.domain.psychology

import com.localdiary.app.model.EmotionAnalysis
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PsychologyAgentContextBuilderTest {
    @Test
    fun `builds bounded context from current diary and recent psychological history`() {
        val context = PsychologyAgentContextBuilder.build(
            currentTitle = "周一复盘",
            currentContent = "今天和同事沟通后一直很紧绷。",
            latestAnalysis = analysis("current", "担心被否定"),
            historicalAnalyses = listOf(
                analysis("old-1", "反复出现过度负责"),
                analysis("old-2", "需要更稳定的边界感"),
                analysis("old-3", "这条应被截断"),
            ),
            maxHistoryItems = 2,
        )

        assertTrue(context.contains("当前日记标题: 周一复盘"))
        assertTrue(context.contains("今天和同事沟通后一直很紧绷。"))
        assertTrue(context.contains("最近心理分析"))
        assertTrue(context.contains("担心被否定"))
        assertTrue(context.contains("历史心理线索"))
        assertTrue(context.contains("反复出现过度负责"))
        assertTrue(context.contains("需要更稳定的边界感"))
        assertFalse(context.contains("这条应被截断"))
    }

    @Test
    fun `builds chat system prompt with non diagnostic boundary`() {
        val prompt = PsychologyAgentContextBuilder.chatSystemPrompt(
            context = "当前日记标题: 周一复盘",
        )

        assertTrue(prompt.contains("专业心理分析 agent"))
        assertTrue(prompt.contains("不能替代心理咨询、医学诊断或危机干预"))
        assertTrue(prompt.contains("当前日记标题: 周一复盘"))
    }

    private fun analysis(id: String, summary: String): EmotionAnalysis = EmotionAnalysis(
        id = id,
        entryId = "entry-$id",
        labels = listOf("焦虑(7/10)"),
        intensity = 70,
        summary = summary,
        suggestions = listOf("先记录触发点"),
        safetyFlag = false,
        createdAt = id.hashCode().toLong(),
        triggers = listOf("沟通压力"),
        cognitivePatterns = listOf("读心推测"),
        needs = listOf("被认可"),
        relationshipSignals = listOf("职场边界"),
        defenseMechanisms = listOf("合理化"),
        strengths = listOf("愿意复盘"),
    )
}
