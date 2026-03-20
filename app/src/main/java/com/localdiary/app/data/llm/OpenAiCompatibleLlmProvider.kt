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
            operationLabel = "主模型连接测试",
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
            operationLabel = "图片模型连接测试",
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
        targetFormat: EntryFormat,
        embeddedImagePlaceholders: List<String>,
    ): ReviewResult {
        val payload = completeText(
            endpoint = config.toMainEndpoint(),
            operationLabel = "格式转换",
            systemPrompt = """
                你是格式转换助手。请仅做格式转换，不改写正文措辞、语气、顺序或语义，不补写、不删减。
                目标格式是 ${targetFormat.label}。如果为达成目标格式必须补充最小量的结构、标签、转义或换行整理，可以做，但不得改变原意。
                ${placeholderInstruction(embeddedImagePlaceholders)}
                请返回 JSON:
                {"issues":["..."],"suggestedTitle":"...","candidateContent":"..."}
                candidateContent 必须严格使用 ${targetFormat.label} 语法。
            """.trimIndent(),
            userPrompt = content,
        )
        val parsed = json.decodeFromString<ReviewPayload>(payload)
        return ReviewResult(
            issues = parsed.issues,
            suggestedTitle = parsed.suggestedTitle,
            candidateContent = parsed.candidateContent,
            format = targetFormat,
        )
    }

    override suspend fun polish(
        config: AiEndpointConfig,
        content: String,
        format: EntryFormat,
        preset: StylePreset,
        embeddedImagePlaceholders: List<String>,
    ): PolishCandidate {
        val payload = completeText(
            endpoint = config.toMainEndpoint(),
            operationLabel = "文风润色",
            systemPrompt = """
                你是文风润色助手。请按以下风格改写，并返回 JSON:
                {"rationale":"...","content":"..."}
                风格提示词: ${preset.prompt}
                只允许调整文风，不允许做格式转换。
                如果原文是普通段落，就继续保持普通段落，不要改成 Markdown 或 HTML。
                如果原文已经是 ${format.label}，只在原有格式层级内润色，不要新增无关的标题、列表或标签。
                ${placeholderInstruction(embeddedImagePlaceholders)}
                输出正文必须保持原有格式形态。
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
        embeddedImagePlaceholders: List<String>,
    ): PsychologyAnalysisResult {
        EmotionPromptTemplate.validate(config.emotionPromptTemplate)?.let { error(it) }

        val limitedImages = imageDataUrls.take(MAX_ANALYSIS_IMAGES)
        val imagePlaceholderHint = if (embeddedImagePlaceholders.isEmpty()) {
            ""
        } else {
            "正文中的 ${embeddedImagePlaceholders.joinToString()} 只是内嵌图片位置标记，不属于正文语义；图片内容以图片线索字段为准。"
        }
        val payload = when {
            imageDataUrls.isEmpty() -> {
                completeText(
                    endpoint = config.toMainEndpoint(),
                    operationLabel = "主模型情绪分析",
                    systemPrompt = EmotionPromptTemplate.render(
                        template = config.emotionPromptTemplate,
                        entryText = content,
                        format = format,
                        imageContext = imagePlaceholderHint,
                    ),
                    userPrompt = "请完成当前文章的情绪分析。",
                )
            }

            config.isImageUnderstandingConfigured -> {
                val imageContext = summarizeImages(
                    config = config,
                    imageDataUrls = limitedImages,
                    endpoint = config.toImageEndpoint(),
                    operationLabel = "图片模型图片理解",
                )
                completeText(
                    endpoint = config.toMainEndpoint(),
                    operationLabel = "主模型情绪分析",
                    systemPrompt = EmotionPromptTemplate.render(
                        template = config.emotionPromptTemplate,
                        entryText = content,
                        format = format,
                        imageContext = listOf(imagePlaceholderHint, imageContext)
                            .filter { it.isNotBlank() }
                            .joinToString(separator = "\n"),
                    ),
                    userPrompt = "请基于正文与图片上下文完成情绪分析。",
                )
            }

            config.supportsVision -> {
                val imageContext = summarizeImages(
                    config = config,
                    imageDataUrls = limitedImages,
                    endpoint = config.toMainEndpoint(),
                    operationLabel = "主模型图片理解",
                )
                completeText(
                    endpoint = config.toMainEndpoint(),
                    operationLabel = "主模型情绪分析",
                    systemPrompt = EmotionPromptTemplate.render(
                        template = config.emotionPromptTemplate,
                        entryText = content,
                        format = format,
                        imageContext = listOf(imagePlaceholderHint, imageContext)
                            .filter { it.isNotBlank() }
                            .joinToString(separator = "\n"),
                    ),
                    userPrompt = "请基于正文与图片上下文完成情绪分析。",
                )
            }

            else -> {
                completeText(
                    endpoint = config.toMainEndpoint(),
                    operationLabel = "主模型情绪分析",
                    systemPrompt = EmotionPromptTemplate.render(
                        template = config.emotionPromptTemplate,
                        entryText = content,
                        format = format,
                        imageContext = listOf(
                            imagePlaceholderHint,
                            "当前文章包含图片，但本次未启用图片理解，仅基于正文分析。",
                        ).filter { it.isNotBlank() }.joinToString(separator = "\n"),
                    ),
                    userPrompt = "请先基于正文完成情绪分析，并在总结里避免编造未分析到的图片细节。",
                )
            }
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
            operationLabel = "主模型周期报告",
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
        endpoint: ResolvedEndpoint,
        operationLabel: String,
    ): String {
        val payload = completeVision(
            endpoint = endpoint,
            operationLabel = operationLabel,
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
        operationLabel: String,
        systemPrompt: String,
        userPrompt: String,
    ): String = executeChatCompletion(
        endpoint = endpoint,
        operationLabel = operationLabel,
        messages = listOf(
            buildTextMessage("system", systemPrompt),
            buildTextMessage("user", userPrompt),
        ),
    )

    private suspend fun completeVision(
        endpoint: ResolvedEndpoint,
        operationLabel: String,
        systemPrompt: String,
        userPrompt: String,
        imageDataUrls: List<String>,
    ): String = executeChatCompletion(
        endpoint = endpoint,
        operationLabel = operationLabel,
        messages = listOf(
            buildTextMessage("system", systemPrompt),
            buildVisionMessage("user", userPrompt, imageDataUrls),
        ),
    )

    private suspend fun executeChatCompletion(
        endpoint: ResolvedEndpoint,
        operationLabel: String,
        messages: List<JsonObject>,
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(endpoint.requestTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(endpoint.requestTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(endpoint.requestTimeoutSeconds.toLong(), TimeUnit.SECONDS)
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
                    error("${operationLabel}失败 (${response.code}): $errorDetail")
                }
                extractResponseContent(responseBody)
            }
        } catch (error: UnknownHostException) {
            error("${operationLabel}失败：无法解析 API 域名，请检查接口地址。")
        } catch (error: SocketTimeoutException) {
            error("${operationLabel}超时。请检查网络，减少图片数量，或提高超时秒数。")
        } catch (error: SerializationException) {
            error("${operationLabel}失败：API 返回了不受支持的数据格式。")
        } catch (error: IOException) {
            error("${operationLabel}失败：${error.message ?: "network error"}")
        }
    }

    private fun extractResponseContent(responseBody: String): String {
        val normalizedBody = responseBody
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = runCatching { json.parseToJsonElement(normalizedBody) }
            .getOrElse { return normalizedBody }
        val rootObject = root as? JsonObject ?: return normalizedBody
        val choice = rootObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: return normalizedBody
        val message = choice["message"]?.jsonObject ?: error("Empty LLM response")
        val content = message["content"] ?: error("Empty LLM response")
        val extracted = when (content) {
            is JsonPrimitive -> content.content
            is JsonArray -> content.joinToString(separator = "\n") { item ->
                val itemObject = item as? JsonObject ?: return@joinToString ""
                itemObject["text"]?.jsonPrimitive?.content
                    ?: itemObject["output_text"]?.jsonPrimitive?.content
                    ?: ""
            }.trim()
            else -> error("Unsupported LLM response content.")
        }
        return extracted
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .ifBlank { error("Empty LLM response") }
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

    private companion object {
        const val MAX_ANALYSIS_IMAGES = 2
    }

    private fun placeholderInstruction(placeholders: List<String>): String {
        if (placeholders.isEmpty()) return ""
        return "原文中的 ${placeholders.joinToString()} 是内嵌图片占位符，必须逐字原样保留，并在输出中继续作为图片位置使用，不得改名、翻译、删除或展开成普通文本。"
    }
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
