package com.localdiary.app.domain.psychology

import com.localdiary.app.model.PsychologyAnalysisResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    fun decodeAnalysisResultOrThrow(raw: String, label: String): PsychologyAnalysisResult {
        val objectText = try {
            extractFirstObject(raw)
        } catch (error: IllegalArgumentException) {
            throw formatError(label, raw, error)
        }
        return try {
            val root = json.parseToJsonElement(objectText).jsonObject
            PsychologyAnalysisResult(
                labels = parseLabels(root["labels"]),
                intensity = root.intValue("intensity"),
                summary = root.stringValue("summary"),
                suggestions = root.stringList("suggestions"),
                safetyFlag = root.booleanValue("safetyFlag"),
                triggers = root.stringList("triggers"),
                cognitivePatterns = root.stringList("cognitivePatterns"),
                needs = root.stringList("needs"),
                relationshipSignals = root.stringList("relationshipSignals"),
                defenseMechanisms = root.stringList("defenseMechanisms"),
                strengths = root.stringList("strengths"),
                bodyStressSignals = root.stringList("bodyStressSignals"),
                riskNotes = root.stringList("riskNotes"),
            )
        } catch (error: SerializationException) {
            throw formatError(label, raw, error)
        } catch (error: IllegalArgumentException) {
            throw formatError(label, raw, error)
        }
    }

    fun extractFirstObject(raw: String): String {
        val text = normalizeJsonLikeText(
            raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim(),
        )
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

    private fun normalizeJsonLikeText(value: String): String = value
        .replace('“', '"')
        .replace('”', '"')
        .replace('＂', '"')
        .replace('，', ',')
        .replace('：', ':')

    private fun parseLabels(element: JsonElement?): List<String> = when (element) {
        is JsonArray -> element.mapNotNull(::parseLabelElement).filter { it.isNotBlank() }
        is JsonPrimitive -> listOfNotNull(element.contentOrNull?.takeIf { it.isNotBlank() })
        else -> emptyList()
    }

    private fun parseLabelElement(element: JsonElement): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonObject -> {
            val name = element.stringValue("name").ifBlank {
                element.stringValue("label").ifBlank { element.stringValue("text") }
            }
            val score = element.optionalIntValue("score") ?: element.optionalIntValue("intensity")
            when {
                name.isBlank() -> null
                score != null -> "$name($score/10)"
                else -> name
            }
        }
        else -> null
    }

    private fun JsonObject.stringValue(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject.intValue(key: String): Int =
        optionalIntValue(key) ?: 0

    private fun JsonObject.optionalIntValue(key: String): Int? {
        val primitive = this[key]?.jsonPrimitive ?: return null
        return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
    }

    private fun JsonObject.booleanValue(key: String): Boolean {
        val primitive = this[key]?.jsonPrimitive ?: return false
        return primitive.booleanOrNull ?: primitive.contentOrNull?.equals("true", ignoreCase = true) == true
    }

    private fun JsonObject.stringList(key: String): List<String> = when (val element = this[key]) {
        is JsonArray -> element.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> item.contentOrNull
                is JsonObject -> item.stringValue("text").ifBlank { item.stringValue("name") }.ifBlank { null }
                else -> null
            }
        }.filter { it.isNotBlank() }
        is JsonPrimitive -> listOfNotNull(element.contentOrNull?.takeIf { it.isNotBlank() })
        else -> emptyList()
    }
}
