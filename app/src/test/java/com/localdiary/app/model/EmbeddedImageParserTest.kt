package com.localdiary.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedImageParserTest {
    @Test
    fun `sanitize replaces embedded images with stable placeholders`() {
        val content = """
            文字前缀
            ![封面](data:image/png;base64,AAAA)
            <img src="data:image/jpeg;base64,BBBB" alt="photo" />
        """.trimIndent()

        val sanitized = EmbeddedImageParser.sanitizeForLlm(content)

        assertEquals(listOf("[[EMBEDDED_IMAGE_1]]", "[[EMBEDDED_IMAGE_2]]"), sanitized.placeholders)
        assertEquals(2, sanitized.imageDataUrls.size)
        assertTrue(sanitized.content.contains("[[EMBEDDED_IMAGE_1]]"))
        assertTrue(sanitized.content.contains("[[EMBEDDED_IMAGE_2]]"))
    }

    @Test
    fun `restore puts data urls back into content`() {
        val original = """![封面](data:image/png;base64,AAAA)"""
        val sanitized = EmbeddedImageParser.sanitizeForLlm(original)

        val restored = EmbeddedImageParser.restorePlaceholders(
            content = "<img src=\"[[EMBEDDED_IMAGE_1]]\" alt=\"封面\" />",
            sanitizedEmbeddedContent = sanitized,
        )

        assertEquals("<img src=\"data:image/png;base64,AAAA\" alt=\"封面\" />", restored)
    }
}
