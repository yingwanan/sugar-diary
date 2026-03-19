package com.localdiary.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddedImageParserTest {
    @Test
    fun `extractDataUrls finds markdown and html inline images`() {
        val content = """
            ![a](data:image/png;base64,AAAA)
            <img src="data:image/jpeg;base64,BBBB" alt="pic" />
        """.trimIndent()

        val urls = EmbeddedImageParser.extractDataUrls(content)

        assertEquals(
            listOf(
                "data:image/png;base64,AAAA",
                "data:image/jpeg;base64,BBBB",
            ),
            urls,
        )
    }

    @Test
    fun `extractDataUrls deduplicates and respects limit`() {
        val content = buildString {
            repeat(5) { index ->
                append("![img$index](data:image/png;base64,AAAA$index)\n")
            }
            append("![dup](data:image/png;base64,AAAA1)")
        }

        val urls = EmbeddedImageParser.extractDataUrls(content, limit = 3)

        assertEquals(
            listOf(
                "data:image/png;base64,AAAA0",
                "data:image/png;base64,AAAA1",
                "data:image/png;base64,AAAA2",
            ),
            urls,
        )
    }
}
