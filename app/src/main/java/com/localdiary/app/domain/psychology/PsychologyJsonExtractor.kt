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

    private fun normalizeJsonLikeText(value: String): String {
        val repaired = StringBuilder(value.length)
        var inString = false
        var openedBySmartQuote = false
        var escaping = false
        for (index in value.indices) {
            val char = value[index]
            if (escaping) {
                repaired.append(char)
                escaping = false
                continue
            }
            when {
                char == '\\' && inString -> {
                    repaired.append(char)
                    escaping = true
                }

                char == '"' -> {
                    repaired.append(char)
                    inString = !inString
                    openedBySmartQuote = false
                }

                isSmartQuote(char) && !inString -> {
                    repaired.append('"')
                    inString = true
                    openedBySmartQuote = true
                }

                isSmartQuote(char) && shouldTreatSmartQuoteAsStringDelimiter(value, index, openedBySmartQuote) -> {
                    repaired.append('"')
                    inString = false
                    openedBySmartQuote = false
                }

                !inString && char == '，' -> repaired.append(',')
                !inString && char == '：' -> repaired.append(':')
                else -> repaired.append(char)
            }
        }
        return repaired.toString()
    }

    private fun isSmartQuote(char: Char): Boolean = char == '“' || char == '”' || char == '＂'

    private fun shouldTreatSmartQuoteAsStringDelimiter(
        value: String,
        quoteIndex: Int,
        openedBySmartQuote: Boolean,
    ): Boolean {
        val nextIndex = nextNonWhitespaceIndex(value, quoteIndex + 1) ?: return false
        return when (value[nextIndex]) {
            ':', '：', '}', ']' -> true
            ',', '，' -> {
                val afterCommaIndex = nextNonWhitespaceIndex(value, nextIndex + 1) ?: return true
                if (!openedBySmartQuote && isSmartQuote(value[afterCommaIndex])) {
                    return quotedTokenIsFollowedByColon(value, afterCommaIndex)
                }
                when (value[afterCommaIndex]) {
                    '"' -> openedBySmartQuote || quotedTokenIsFollowedByColon(value, afterCommaIndex)
                    '“', '”', '＂', '{', '}', '[', ']' -> true
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun quotedTokenIsFollowedByColon(value: String, quoteIndex: Int): Boolean {
        val quote = value[quoteIndex]
        var escaping = false
        for (index in quoteIndex + 1 until value.length) {
            val char = value[index]
            if (escaping) {
                escaping = false
                continue
            }
            if (char == '\\') {
                escaping = true
                continue
            }
            val closesQuotedToken = when (quote) {
                '"' -> char == '"'
                else -> isSmartQuote(char) || char == '"'
            }
            if (closesQuotedToken) {
                val nextIndex = nextNonWhitespaceIndex(value, index + 1) ?: return false
                return value[nextIndex] == ':' || value[nextIndex] == '：'
            }
        }
        return false
    }

    private fun nextNonWhitespaceIndex(value: String, startIndex: Int): Int? {
        for (index in startIndex until value.length) {
            if (!value[index].isWhitespace()) return index
        }
        return null
    }

    private fun parseLabels(element: JsonElement?): List<String> = when (element) {
        is JsonArray -> PsychologyLabelNormalizer.normalizeLabels(element.mapNotNull(::parseLabelElement))
        is JsonPrimitive -> PsychologyLabelNormalizer.normalizeLabels(listOfNotNull(element.contentOrNull))
        else -> emptyList()
    }

    private fun parseLabelElement(element: JsonElement): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonObject -> {
            val name = element.stringValue("name").ifBlank {
                element.stringValue("label").ifBlank { element.stringValue("text") }
            }
            when {
                name.isBlank() -> null
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
