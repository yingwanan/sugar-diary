package com.localdiary.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionPromptTemplateTest {
    @Test
    fun `validate accepts default template`() {
        val error = EmotionPromptTemplate.validate(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE)

        assertNull(error)
    }

    @Test
    fun `validate rejects template without required placeholders`() {
        val error = EmotionPromptTemplate.validate(
            """
                仅保留 ${AiEndpointConfig.ENTRY_TEXT_PLACEHOLDER}
                {"labels":["..."],"intensity":0-100,"summary":"...","suggestions":["..."],"safetyFlag":false}
            """.trimIndent(),
        )

        assertEquals(
            "心理分析提示词缺少变量：${AiEndpointConfig.ENTRY_FORMAT_PLACEHOLDER}, ${AiEndpointConfig.IMAGE_CONTEXT_PLACEHOLDER}",
            error,
        )
    }

    @Test
    fun `render replaces placeholders with entry content format and image context`() {
        val rendered = EmotionPromptTemplate.render(
            template = AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE,
            entryText = "今天看完海边日落。",
            format = EntryFormat.MARKDOWN,
            imageContext = "图片里是黄昏海面和独自站立的人。",
        )

        assertTrue(rendered.contains("今天看完海边日落。"))
        assertTrue(rendered.contains(EntryFormat.MARKDOWN.label))
        assertTrue(rendered.contains("图片里是黄昏海面和独自站立的人。"))
    }
}
