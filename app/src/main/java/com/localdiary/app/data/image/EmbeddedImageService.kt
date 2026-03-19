package com.localdiary.app.data.image

import android.content.ContentResolver
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.localdiary.app.model.EntryFormat
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmbeddedImageService(
    private val contentResolver: ContentResolver,
) {
    suspend fun createSnippet(uri: Uri, format: EntryFormat): EmbeddedImageInsertResult = withContext(Dispatchers.IO) {
        val sourceBytes = readSourceBytes(uri)
        val mimeType = resolveMimeType(uri, sourceBytes)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            error("无法识别所选图片格式。")
        }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = ARGB_8888
        }
        val decodedBitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, decodeOptions)
            ?: error("图片解码失败，请确认所选文件是受支持的图片。")

        val scaledBitmap = decodedBitmap.scaleDownIfNeeded(MAX_DIMENSION)
        if (scaledBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }

        try {
            val compressed = compressBitmap(
                bitmap = scaledBitmap,
                preferPng = mimeType.contains("png", ignoreCase = true) || scaledBitmap.hasAlpha(),
            )
            val dataUrl = buildDataUrl(compressed.mimeType, compressed.bytes)
            EmbeddedImageInsertResult(
                rawSnippet = formatSnippet(dataUrl, format),
                dataUrl = dataUrl,
                mimeType = compressed.mimeType,
                byteSize = compressed.bytes.size,
            )
        } finally {
            scaledBitmap.recycle()
        }
    }

    private fun readSourceBytes(uri: Uri): ByteArray {
        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
                }
        } catch (_: SecurityException) {
            error("没有读取所选图片的权限，请重新选择图片。")
        }
        if (bytes == null || bytes.isEmpty()) {
            error("无法读取所选图片。请重新选择，或改用系统文件应用中的图片。")
        }
        return bytes
    }

    private fun resolveMimeType(uri: Uri, sourceBytes: ByteArray): String {
        val mimeType = contentResolver.getType(uri).orEmpty()
        if (mimeType.startsWith("image/")) return mimeType

        val displayName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            .orEmpty()
        EntryFormatImageType.fromFileName(displayName)?.let { return it.mimeType }
        EntryFormatImageType.fromHeader(sourceBytes)?.let { return it.mimeType }
        error("无法识别图片类型，请选择 PNG、JPEG、WEBP 或 GIF 图片。")
    }

    private fun compressBitmap(bitmap: Bitmap, preferPng: Boolean): EncodedImage {
        if (preferPng) {
            val pngBytes = ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }
            if (pngBytes.size <= MAX_IMAGE_BYTES) {
                return EncodedImage("image/png", pngBytes)
            }
        }

        var quality = 92
        while (quality >= 55) {
            val jpegBytes = ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.toByteArray()
            }
            if (jpegBytes.size <= MAX_IMAGE_BYTES) {
                return EncodedImage("image/jpeg", jpegBytes)
            }
            quality -= 12
        }
        error("图片过大，压缩后仍超过 ${MAX_IMAGE_BYTES / 1024} KB。请换一张更小的图片。")
    }

    private fun buildDataUrl(mimeType: String, bytes: ByteArray): String {
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:$mimeType;base64,$encoded"
    }

    private fun formatSnippet(dataUrl: String, format: EntryFormat): String = when (format) {
        EntryFormat.MARKDOWN -> "\n![本地图片]($dataUrl)\n"
        EntryFormat.HTML -> "\n<img src=\"$dataUrl\" alt=\"本地图片\" />\n"
    }

    private fun Bitmap.scaleDownIfNeeded(maxDimension: Int): Bitmap {
        val currentMax = max(width, height)
        if (currentMax <= maxDimension) return this

        val ratio = maxDimension.toFloat() / currentMax.toFloat()
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxDimension * 2 || height / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private data class EncodedImage(
        val mimeType: String,
        val bytes: ByteArray,
    )

    private enum class EntryFormatImageType(val mimeType: String) {
        PNG("image/png"),
        JPEG("image/jpeg"),
        WEBP("image/webp"),
        GIF("image/gif"),
        ;

        companion object {
            fun fromFileName(fileName: String): EntryFormatImageType? = when {
                fileName.endsWith(".png", ignoreCase = true) -> PNG
                fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> JPEG
                fileName.endsWith(".webp", ignoreCase = true) -> WEBP
                fileName.endsWith(".gif", ignoreCase = true) -> GIF
                else -> null
            }

            fun fromHeader(bytes: ByteArray): EntryFormatImageType? = when {
                bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> PNG
                bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> JPEG
                bytes.size >= 12 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF" &&
                    bytes.copyOfRange(8, 12).decodeToString() == "WEBP" -> WEBP
                bytes.size >= 6 && bytes.copyOfRange(0, 3).decodeToString() == "GIF" -> GIF
                else -> null
            }
        }
    }

    private companion object {
        const val MAX_DIMENSION = 1600
        const val MAX_IMAGE_BYTES = 900 * 1024
    }
}
