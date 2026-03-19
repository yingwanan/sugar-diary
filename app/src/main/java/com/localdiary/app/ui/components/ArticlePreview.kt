package com.localdiary.app.ui.components

import android.graphics.Color
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.localdiary.app.model.EntryFormat
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

@Composable
fun ArticlePreview(
    content: String,
    format: EntryFormat,
    modifier: Modifier = Modifier,
) {
    val parser = remember { Parser.builder().build() }
    val renderer = remember { HtmlRenderer.builder().build() }
    val htmlBody = remember(content, format) {
        when (format) {
            EntryFormat.MARKDOWN -> renderer.render(parser.parse(content))
            EntryFormat.HTML -> content
        }
    }
    val html = remember(htmlBody) {
        """
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent;
                    color: #1f2937;
                    font-family: sans-serif;
                    line-height: 1.65;
                    word-break: break-word;
                    overflow-wrap: anywhere;
                }
                body {
                    padding: 12px 14px 24px;
                }
                img {
                    display: block;
                    max-width: 100%;
                    width: auto;
                    height: auto;
                    max-height: 72vh;
                    object-fit: contain;
                    margin: 16px auto;
                    border-radius: 16px;
                }
                p, ul, ol, blockquote, pre, h1, h2, h3, h4, h5, h6 {
                    margin-top: 0;
                    margin-bottom: 12px;
                }
                pre {
                    white-space: pre-wrap;
                }
            </style>
        </head>
        <body>$htmlBody</body>
        </html>
        """.trimIndent()
    }
    var lastHtml by remember { mutableStateOf("") }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.javaScriptEnabled = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            }
        },
        update = { view ->
            if (lastHtml != html) {
                lastHtml = html
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
    )
}
