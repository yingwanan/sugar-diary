package com.localdiary.app.ui.components

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.localdiary.app.model.EditorDocumentBlock
import com.localdiary.app.model.EditorDocumentParser
import com.localdiary.app.model.EntryFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

@Composable
fun ArticlePreview(
    content: String,
    format: EntryFormat,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(content) { EditorDocumentParser.parse(content) }
    val parser = remember { Parser.builder().build() }
    val renderer = remember { HtmlRenderer.builder().build() }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = blocks,
            key = { _, block -> block.id },
            contentType = { _, block ->
                when (block) {
                    is EditorDocumentBlock.Text -> "text"
                    is EditorDocumentBlock.Image -> "image"
                }
            },
        ) { index, block ->
            when (block) {
                is EditorDocumentBlock.Text -> ArticleTextBlock(
                    text = block.text,
                    format = format,
                    parser = parser,
                    renderer = renderer,
                )

                is EditorDocumentBlock.Image -> EmbeddedImagePreview(
                    dataUrl = block.dataUrl,
                    title = "内嵌图片 ${index + 1}",
                    subtitle = block.mimeType,
                )
            }
        }
    }
}

@Composable
private fun ArticleTextBlock(
    text: String,
    format: EntryFormat,
    parser: Parser,
    renderer: HtmlRenderer,
) {
    if (text.isBlank()) return

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val html = remember(text, format) {
        when (format) {
            EntryFormat.MARKDOWN -> renderer.render(parser.parse(text))
            EntryFormat.HTML -> text
        }
    }

    AndroidView(
        factory = { context ->
            TextView(context).apply {
                textSize = 16f
                setLineSpacing(0f, 1.35f)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        },
    )
}
