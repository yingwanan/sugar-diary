package com.localdiary.app.ui.components

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val parser = remember { Parser.builder().build() }
    val renderer = remember { HtmlRenderer.builder().build() }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val html = remember(markdown) { renderer.render(parser.parse(markdown)) }
    AndroidView(
        modifier = modifier,
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
