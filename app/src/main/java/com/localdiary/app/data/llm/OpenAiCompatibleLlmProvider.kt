package com.localdiary.app.data.llm

import com.localdiary.app.model.AiEndpointConfig
import com.localdiary.app.model.EmotionPromptTemplate
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.PeriodicReportResult
import com.localdiary.app.model.PolishCandidate
import com.localdiary.app.model.PsychologyAnalysisResult
import com.localdiary.app.model.ReportPeriod
import com.localdiary.app.model.ReviewResult
import com.localdiary.app.model.StylePreset
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiCompatibleLlmProvider(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : LlmProvider {
    override suspend fun testConnection(config: AiEndpointConfig): String {
        val payload = completeText(
            endpoint = config.toMainEndpoint(),
            systemPrompt = """
                你是连接测试助手。请仅返回 JSON:
                {"status":"ok","message":"..."}
            """.trimIndent(),
            userPrompt = "请确认当前模型可以正常响应。",
        )
        val parsed = json.decodeFromString<TestConnectionPayload>(payload)
        return parsed.message.ifBlank { "连接成功。" }
    }

    override suspend fun testImageConnection(config: AiEndpointConfig): String {
        val payload = completeText(
            endpoint = config.toImageEndpoint(),
            systemPrompt = """
                你是连接测试助手。请仅返回 JSON:
                {"status":"ok","message":"..."}
            """.trimIndent(),
            userPrompt = "请确认当前图片理解模型可以正常响应。",
        )
        val parsed = json.decodeFromString<TestConnectionPayload>(payload)
        return parsed.message.ifBlank { "图片理解模型连接成功。" }
    }

    override suspend fun review(
        config: AiEndpointConfig,
        content: String,
        format: EntryFormat,
    ): ReviewResult {
        val payload = completeText(
            endpoint = config.toMainEndpoint(),
            systemPrompt = """
                你是文章审校助手。请返回 JSON:
                {"issues":["..."],"suggestedTitle":"...","candidateContent":"..."}
                candidateContent 必须严格使用 ${format.label} 语法。
            """.trimIndent(),
            userPrompt = content,
        )
        val parsed = json.decodeFromString<ReviewPayload>(payload)
        return ReviewResult(
            issues = parsed.issues,
            suggestedTitle = parsed.suggestedTitle,
            candidateContent = parsed.candidateContent,
            format = format,
        )
    }

    override suspend fun polish(
        config: AiEndpointConfig,
        content: String,
        format: EntryFormat,
        preset: StylePreset,
    ): PolishCandidate {
        val payload = completeText(
            endpoint = config.toMainEndpoint(),
            systemPrompt = """
                你是文风润色助手。请按以下风格改写，并返回 JSON:
                {"rationale":"...","content":"..."}
                风格提示词: ${preset.prompt}
                输出正文必须严格使用 ${format.label} 语法。
            """.trimIndent(),
            userPrompt = content,
        )
        val parsed = json.decodeFromString<PolishPayload>(payload)
        return PolishCandidate(
            styleName = preset.name,
            rationale = parsed.rationale,
            content = parsed.content,
            format = format,
        )
    }

    override suspend fun analyzePsychology(
        config: AiEndpointConfig,
        content: String,
        format: EntryFormat,
        imageDataUrls: List<String>,
    ): PsychologyAnalysisResult {
        EmotionPromptTemplate.validate(config.emotionPromptTemplate)?.let { error(it) }

        val payload = when {
            imageDataUrls.isEmpty() -> {
                completeText(
                    endpoint = config.toMainEndpoint(),
                    systemPrompt = EmotionPromptTemplate.render(
                        template = config.emotionPromptTemplate,
                        entryText = content,
                        format = format,
                        imageContext = "",
                    ),
                    userPrompt = "请完成当前文章的情绪分析。",
                )
            }

            config.supportsVision -> {
                completeVision(
                    endpoint = config.toMainEndpoint(),
                    systemPrompt = EmotionPromptTemplate.render(
                        template = config.emotionPromptTemplate,
                        entryText = content,
                        format = format,
                        imageContext = "图片将与正文一起直接发送给模型，请结合图片内容分析。",
                    ),
                    userPrompt = "请结合正文与这些图片完成情绪分析。",
                    imageDataUrls = imageDataUrls,
                )
            }

            config.isImageUnderstandingConfigured -> {
                val imageContext = summarizeImages(config, imageDataUrls)
                completeText(
                    endpoint = config.toMainEndpoint(),
                    systemPrompt = EmotionPromptTemplate.render(
                        template = config.emotionPromptTemplate,
                        entryText = content,
                        format = format,
                        imageContext = imageContext,
                    ),
                    userPrompt = "请基于正文与图片上下文完成情绪分析。",
                )
            }

            else -> error("当前文章包含图片，但主模型未开启视觉能力，且未配置图片理解模型。")
        }
        return json.decodeFromString(payload)
    }

    override suspend fun summarizePeriod(
        config: AiEndpointConfig,
        period: ReportPeriod,
        summaries: List<String>,
    ): PeriodicReportResult {
        val payload = completeText(
            endpoint = config.toMainEndpoint(),
            systemPrompt = """
                你是周期情绪报告助手。请基于已有分析摘要生成非医疗建议，返回 JSON:
                {"dominantMoods":["..."],"summary":"...","advice":["..."]}
                周期为 $period。
            """.trimIndent(),
            userPrompt = summaries.joinToString(separator = "\n\n"),
        )
        return json.decodeFromString(payload)
    }

    private suspend fun summarizeImages(
        config: AiEndpointConfig,
        imageDataUrls: List<String>,
    ): String {
        val payload = completeVision(
            endpoint = config.toImageEndpoint(),
            systemPrompt = """
                你是图片理解助手。请仅返回 JSON:
                {"summary":"..."}
                需要提炼画面主体、环境线索、人物状态，以及能辅助情绪分析的视觉细节。
            """.trimIndent(),
            userPrompt = "请概括这些图片里和情绪分析相关的内容。",
            imageDataUrls = imageDataUrls,
        )
        return json.decodeFromString<ImageSummaryPayload>(payload).summary
    }

    private suspend fun completeText(
        endpoint: ResolvedEndpoint,
        systemPrompt: String,
        userPrompt: String,
    ): String = executeChatCompletion(
        endpoint = endpoint,
        messages = listOf(
            buildTextMessage("system", systemPrompt),
            buildTextMessage("user", userPrompt),
        ),
    )

    private suspend fun completeVision(
        endpoint: ResolvedEndpoint,
        systemPrompt: String,
        userPrompt: String,
        imageDataUrls: List<String>,
    ): String = executeChatCompletion(
        endpoint = endpoint,
        messages = listOf(
            buildTextMessage("system", systemPrompt),
            buildVisionMessage("user", userPrompt, imageDataUrls),
        ),
    )

    private suspend fun executeChatCompletion(
        endpoint: ResolvedEndpoint,
        messages: List<JsonObject>,
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .callTimeout(endpoint.requestTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

        val body = json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("model", JsonPrimitive(endpoint.model))
                put(
                    "messages",
                    JsonArray(messages),
                )
                put("temperature", JsonPrimitive(0.3))
                put(
                    "response_format",
                    buildJsonObject {
                        put("type", JsonPrimitive("json_object"))
                    },
                )
            },
        )

        val request = Request.Builder()
            .url(endpoint.url)
            .header("Authorization", "Bearer ${endpoint.apiKey}")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorDetail = responseBody.take(200).ifBlank { "No response body" }
                    error("LLM request failed (${response.code}): $errorDetail")
                }
                extractResponseContent(responseBody)
            }
        } catch (error: UnknownHostException) {
            error("Unable to resolve the API host. Check the interface address.")
        } catch (error: SocketTimeoutException) {
            error("The API request timed out. Check the network or increase the timeout.")
        } catch (error: SerializationException) {
            error("The API returned data in an unsupported format.")
        } catch (error: IOException) {
            error("The API request could not be completed: ${error.message ?: "network error"}")
        }
    }

    private fun extractResponseContent(responseBody: String): String {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: error("Empty LLM response")
        val message = choice["message"]?.jsonObject ?: error("Empty LLM response")
        val content = message["content"] ?: error("Empty LLM response")
        return when (content) {
            is JsonPrimitive -> content.content
            is JsonArray -> content.joinToString(separator = "\n") { item ->
                item.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
            }.trim()
            else -> error("Unsupported LLM response content.")
        }.ifBlank { error("Empty LLM response") }
    }

    private fun buildTextMessage(role: String, text: String): JsonObject = buildJsonObject {
        put("role", JsonPrimitive(role))
        put("content", JsonPrimitive(text))
    }

    private fun buildVisionMessage(
        role: String,
        text: String,
        imageDataUrls: List<String>,
    ): JsonObject = buildJsonObject {
        put("role", JsonPrimitive(role))
        put(
            "content",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", JsonPrimitive("text"))
                        put("text", JsonPrimitive(text))
                    },
                )
                imageDataUrls.forEach { imageDataUrl ->
                    add(
                        buildJsonObject {
                            put("type", JsonPrimitive("image_url"))
                            put(
                                "image_url",
                                buildJsonObject {
                                    put("url", JsonPrimitive(imageDataUrl))
                                },
                            )
                        },
                    )
                }
            },
        )
    }

    private fun AiEndpointConfig.toMainEndpoint(): ResolvedEndpoint {
        require(isConfigured) { "LLM endpoint is not configured." }
        return ResolvedEndpoint(
            url = normalizedChatCompletionsUrl(),
            apiKey = apiKey,
            model = model,
            requestTimeoutSeconds = requestTimeoutSeconds,
        )
    }

    private fun AiEndpointConfig.toImageEndpoint(): ResolvedEndpoint {
        require(isImageUnderstandingConfigured) { "Image understanding model is not configured." }
        return ResolvedEndpoint(
            url = normalizedImageChatCompletionsUrl(),
            apiKey = imageApiKey,
            model = imageModel,
            requestTimeoutSeconds = requestTimeoutSeconds,
        )
    }

    private data class ResolvedEndpoint(
        val url: String,
        val apiKey: String,
        val model: String,
        val requestTimeoutSeconds: Int,
    )
}

@Serializable
private data class ReviewPayload(
    val issues: List<String>,
    val suggestedTitle: String? = null,
    val candidateContent: String,
)

@Serializable
private data class PolishPayload(
    val rationale: String,
    val content: String,
)

@Serializable
private data class TestConnectionPayload(
    val status: String,
    val message: String = "",
)

@Serializable
private data class ImageSummaryPayload(
    val summary: String,
)
