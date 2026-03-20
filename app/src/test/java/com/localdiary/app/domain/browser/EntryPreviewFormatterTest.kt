package com.localdiary.app.domain.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryPreviewFormatterTest {
    @Test
    fun `build preview strips embedded image payloads and markdown decorations`() {
        val preview = EntryPreviewFormatter.buildPreview(
            """
            # 标题
            今天很平静。
            ![插图](data:image/png;base64,AAAA)
            [继续阅读](https://example.com)
            """.trimIndent(),
        )

        assertTrue(preview.contains("标题"))
        assertTrue(preview.contains("今天很平静"))
        assertTrue(preview.contains("继续阅读"))
        assertFalse(preview.contains("data:image"))
        assertFalse(preview.contains("#"))
    }

    @Test
    fun `build preview returns fallback when content becomes empty`() {
        assertEquals(
            "暂无正文摘要",
            EntryPreviewFormatter.buildPreview("""<img src="data:image/png;base64,AAAA" />"""),
        )
    }
}
