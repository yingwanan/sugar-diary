package com.localdiary.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryFormatTest {
    @Test
    fun `fromFileName detects markdown and html files`() {
        assertEquals(EntryFormat.MARKDOWN, EntryFormat.fromFileName("note.md"))
        assertEquals(EntryFormat.HTML, EntryFormat.fromFileName("note.html"))
        assertEquals(EntryFormat.HTML, EntryFormat.fromFileName("note.HTM"))
    }

    @Test
    fun `fromFileName returns null for unsupported files`() {
        assertNull(EntryFormat.fromFileName("note.txt"))
    }
}
