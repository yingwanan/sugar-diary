package com.localdiary.app.domain.psychology

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object PsychologyJsonExtractor {
    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    inline fun <reified T> decodeOrThrow(raw: String, label: String): T {
        val objectText = try {
            extractFirstObject(raw)
        } catch (error: IllegalArgumentException) {
            throw formatError(label, raw, error)
        }
        return try {
            json.decodeFromString(objectText)
        } catch (error: SerializationException) {
            throw formatError(label, raw, error)
        } catch (error: IllegalArgumentException) {
            throw formatError(label, raw, error)
        }
    }

    fun extractFirstObject(raw: String): String {
        val text = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        var start = -1
        var depth = 0
        var inString = false
        var escaping = false
        for (index in text.indices) {
            val char = text[index]
            if (start < 0) {
                if (char == '{') {
                    start = index
                    depth = 1
                }
                continue
            }
            if (escaping) {
                escaping = false
                continue
            }
            when (char) {
                '\\' -> if (inString) escaping = true
                '"' -> inString = !inString
                '{' -> if (!inString) depth += 1
                '}' -> if (!inString) {
                    depth -= 1
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        throw IllegalArgumentException("No balanced JSON object found")
    }

    fun preview(raw: String, maxLength: Int = 240): String = raw
        .replace(Regex("\\s+"), " ")
        .trim()
        .let { if (it.length <= maxLength) it else it.take(maxLength) + "…" }

    fun formatError(label: String, raw: String, cause: Throwable): IllegalStateException =
        IllegalStateException("${label}返回格式不正确。返回内容预览：${preview(raw)}", cause)
}
