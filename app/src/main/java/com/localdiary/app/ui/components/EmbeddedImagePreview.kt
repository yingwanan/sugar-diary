package com.localdiary.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EmbeddedImagePreview(
    dataUrl: String,
    modifier: Modifier = Modifier,
    title: String = "本地图片",
    subtitle: String? = null,
    footer: @Composable (() -> Unit)? = null,
) {
    var bitmap by remember(dataUrl) { mutableStateOf<Bitmap?>(EmbeddedImageThumbnailCache[dataUrl]) }
    LaunchedEffect(dataUrl) {
        if (bitmap == null) {
            bitmap = EmbeddedImageThumbnailCache.load(dataUrl)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val currentBitmap = bitmap
                    if (currentBitmap != null) {
                        val targetHeight = (maxWidth * currentBitmap.height.toFloat() / currentBitmap.width.toFloat())
                            .coerceIn(140.dp, 420.dp)
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(targetHeight)
                                .heightIn(max = 420.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("正在生成缩略图…")
                        }
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            footer?.invoke()
        }
    }
}

private object EmbeddedImageThumbnailCache {
    private val cache = LruCache<String, Bitmap>(16)

    operator fun get(dataUrl: String): Bitmap? = cache.get(dataUrl)

    suspend fun load(dataUrl: String): Bitmap? = withContext(Dispatchers.Default) {
        cache.get(dataUrl)?.let { return@withContext it }
        val payload = dataUrl.substringAfter("base64,", "")
        if (payload.isBlank()) return@withContext null
        val decoded = runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull() ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(decoded, 0, decoded.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 480)
        }
        val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size, options) ?: return@withContext null
        cache.put(dataUrl, bitmap)
        bitmap
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sample = 1
        while (width / sample > targetSize * 2 || height / sample > targetSize * 2) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
