package com.localdiary.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDocumentParserTest {
    @Test
    fun `parse markdown image into text and image blocks`() {
        val content = "before\n![img](data:image/png;base64,AAAA)\nafter"

        val blocks = EditorDocumentParser.parse(content)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is EditorDocumentBlock.Text)
        assertTrue(blocks[1] is EditorDocumentBlock.Image)
        assertTrue(blocks[2] is EditorDocumentBlock.Text)
        assertEquals(content, EditorDocumentParser.buildContent(blocks))
    }

    @Test
    fun `parse html image and keep trailing text block`() {
        val content = "<img src=\"data:image/jpeg;base64,BBBB\" alt=\"x\" />"

        val blocks = EditorDocumentParser.parse(content)

        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is EditorDocumentBlock.Image)
        assertTrue(blocks[1] is EditorDocumentBlock.Text)
        assertEquals(content, EditorDocumentParser.buildContent(blocks))
    }

    @Test
    fun `build content preserves mixed images and text`() {
        val content = "a![one](data:image/png;base64,AAAA)b<img src=\"data:image/webp;base64,CCCC\" />c"

        val rebuilt = EditorDocumentParser.buildContent(EditorDocumentParser.parse(content))

        assertEquals(content, rebuilt)
    }
}
