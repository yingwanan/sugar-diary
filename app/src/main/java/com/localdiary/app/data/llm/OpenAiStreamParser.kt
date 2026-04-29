package com.localdiary.app.data.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object OpenAiStreamParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseDataLine(data: String): String? {
        val trimmed = data.trim()
        if (trimmed.isBlank() || trimmed == "[DONE]") return null
        val root = runCatching { json.parseToJsonElement(trimmed).jsonObject }.getOrNull() ?: return null
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val delta = choice["delta"]?.jsonObject
        val message = choice["message"]?.jsonObject
        val content = delta?.get("content") ?: message?.get("content") ?: return null
        return when (content) {
            is JsonPrimitive -> content.contentOrNull
            is JsonArray -> content.mapNotNull { item ->
                (item as? JsonObject)?.get("text") as? JsonPrimitive
            }.mapNotNull { it.contentOrNull }.joinToString("")
            else -> null
        }?.takeIf { it.isNotEmpty() }
    }
}
