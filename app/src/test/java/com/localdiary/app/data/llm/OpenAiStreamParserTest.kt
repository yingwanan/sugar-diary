package com.localdiary.app.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenAiStreamParserTest {
    @Test
    fun `parses chat completion delta content from sse data line`() {
        val content = OpenAiStreamParser.parseDataLine(
            "{\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}",
        )

        assertEquals("你好", content)
    }

    @Test
    fun `ignores done and chunks without content`() {
        assertNull(OpenAiStreamParser.parseDataLine("[DONE]"))
        assertNull(OpenAiStreamParser.parseDataLine("{\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}"))
    }
}
