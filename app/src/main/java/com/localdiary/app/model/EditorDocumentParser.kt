package com.localdiary.app.model

import java.util.UUID
import kotlin.math.ceil

object EditorDocumentParser {
    private val markdownImageRegex = Regex(
        """!\[[^\]]*]\((data:image/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=\r\n]+)\)""",
    )
    private val htmlImageRegex = Regex(
        """<img\b[^>]*src=["'](data:image/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=\r\n]+)["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(content: String): List<EditorDocumentBlock> {
        if (content.isEmpty()) return listOf(EditorDocumentBlock.Text(id = newTextId(), text = ""))

        val imageMatches = (markdownImageRegex.findAll(content) + htmlImageRegex.findAll(content))
            .map { match ->
                ImageMatch(
                    start = match.range.first,
                    endExclusive = match.range.last + 1,
                    rawSnippet = match.value,
                    dataUrl = match.groupValues[1].replace("\n", "").replace("\r", ""),
                )
            }
            .sortedBy { it.start }
            .fold(mutableListOf<ImageMatch>()) { acc, item ->
                val previous = acc.lastOrNull()
                if (previous == null || item.start >= previous.endExclusive) {
                    acc += item
                }
                acc
            }

        val blocks = mutableListOf<EditorDocumentBlock>()
        var cursor = 0
        imageMatches.forEach { match ->
            if (match.start > cursor) {
                blocks += EditorDocumentBlock.Text(
                    id = newTextId(),
                    text = content.substring(cursor, match.start),
                )
            }
            blocks += EditorDocumentBlock.Image(
                id = newImageId(),
                rawSnippet = match.rawSnippet,
                dataUrl = match.dataUrl,
                mimeType = extractMimeType(match.dataUrl),
                byteSize = estimateByteSize(match.dataUrl),
            )
            cursor = match.endExclusive
        }
        if (cursor < content.length) {
            blocks += EditorDocumentBlock.Text(
                id = newTextId(),
                text = content.substring(cursor),
            )
        }
        if (blocks.isEmpty()) {
            blocks += EditorDocumentBlock.Text(id = newTextId(), text = "")
        }
        if (blocks.last() !is EditorDocumentBlock.Text) {
            blocks += EditorDocumentBlock.Text(id = newTextId(), text = "")
        }
        return blocks
    }

    fun buildContent(blocks: List<EditorDocumentBlock>): String = buildString {
        blocks.forEach { block ->
            when (block) {
                is EditorDocumentBlock.Text -> append(block.text)
                is EditorDocumentBlock.Image -> append(block.rawSnippet)
            }
        }
    }

    fun createImageBlock(
        rawSnippet: String,
        dataUrl: String,
        mimeType: String,
        byteSize: Int,
    ): EditorDocumentBlock.Image = EditorDocumentBlock.Image(
        id = newImageId(),
        rawSnippet = rawSnippet,
        dataUrl = dataUrl,
        mimeType = mimeType,
        byteSize = byteSize,
    )

    fun newTextBlock(text: String = ""): EditorDocumentBlock.Text =
        EditorDocumentBlock.Text(id = newTextId(), text = text)

    private fun extractMimeType(dataUrl: String): String =
        dataUrl.substringAfter("data:", "").substringBefore(";base64", missingDelimiterValue = "image/*")

    private fun estimateByteSize(dataUrl: String): Int {
        val payload = dataUrl.substringAfter("base64,", "")
        val sanitized = payload.trim()
        if (sanitized.isEmpty()) return 0
        val padding = sanitized.takeLastWhile { it == '=' }.length
        return ceil((sanitized.length * 3) / 4.0).toInt() - padding
    }

    private fun newTextId(): String = "text-${UUID.randomUUID()}"

    private fun newImageId(): String = "image-${UUID.randomUUID()}"

    private data class ImageMatch(
        val start: Int,
        val endExclusive: Int,
        val rawSnippet: String,
        val dataUrl: String,
    )
}
