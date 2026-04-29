package com.localdiary.app.domain.psychology

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PsychologyJsonExtractorTest {
    @Test
    fun `decodes pure json object`() {
        val payload = PsychologyJsonExtractor.decodeOrThrow<SamplePayload>(
            raw = "{\"name\":\"ok\",\"items\":[\"a\"]}",
            label = "测试",
        )

        assertEquals("ok", payload.name)
        assertEquals(listOf("a"), payload.items)
    }

    @Test
    fun `decodes fenced json object with surrounding text`() {
        val payload = PsychologyJsonExtractor.decodeOrThrow<SamplePayload>(
            raw = "这是分析结果：\n```json\n{\"name\":\"ok\",\"items\":[\"a **b**\"]}\n```\n请查收。",
            label = "测试",
        )

        assertEquals("ok", payload.name)
        assertEquals(listOf("a **b**"), payload.items)
    }

    @Test
    fun `decodes first balanced object when markdown text contains braces later`() {
        val payload = PsychologyJsonExtractor.decodeOrThrow<SamplePayload>(
            raw = "前缀 {\"name\":\"ok\",\"items\":[\"包含 { 不是对象边界 } 的文本\"]} 后缀 {ignored}",
            label = "测试",
        )

        assertEquals(listOf("包含 { 不是对象边界 } 的文本"), payload.items)
    }

    @Test
    fun `error includes preview for invalid payload`() {
        val error = runCatching {
            PsychologyJsonExtractor.decodeOrThrow<SamplePayload>("没有 JSON", "最终心理分析")
        }.exceptionOrNull()

        assertTrue(error!!.message!!.contains("最终心理分析返回格式不正确"))
        assertTrue(error.message!!.contains("没有 JSON"))
    }

    @Serializable
    private data class SamplePayload(
        val name: String,
        val items: List<String>,
    )
}

class PsychologyAnalysisResultJsonCompatibilityTest {
    @Test
    fun `decodes analysis result when labels are scored objects and closing quote is smart quote`() {
        val result = PsychologyJsonExtractor.decodeAnalysisResultOrThrow(
            """
                {"labels":[{"name":"愧疚与自责”，“score":8},{"name":"怀旧性悲伤","score":7}],"intensity":82,"summary":"**整体**处在反刍中","suggestions":["先暂停自责"],"safetyFlag":false,"triggers":["旧关系回忆"]}
            """.trimIndent(),
            "最终心理分析",
        )

        assertEquals(listOf("愧疚与自责(8/10)", "怀旧性悲伤(7/10)"), result.labels)
        assertEquals(82, result.intensity)
        assertEquals(listOf("旧关系回忆"), result.triggers)
        assertEquals("**整体**处在反刍中", result.summary)
    }
}
