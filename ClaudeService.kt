package com.aiworkgroup.data.api

import com.aiworkgroup.config.AIProvider
import com.aiworkgroup.config.AIProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ClaudeService(
    private val apiKey: String,
    override val modelName: String = "claude-3-sonnet-20240229",
    private val baseUrl: String = "https://api.anthropic.com/v1",
    okHttpClient: OkHttpClient = OkHttpClient()
) : AIService, ConfigurableAIService {

    override val providerName: String = "Claude"
    override val config: AIProviderConfig = AIProviderConfig(
        provider = AIProvider.CLAUDE,
        apiKey = apiKey,
        baseUrl = baseUrl,
        modelName = modelName
    )

    private val client = okHttpClient
    private val json = Json { ignoreUnknownKeys = true }

    override fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String?
    ): Flow<String> = flow {
        val claudeMessages = messages.map { msg ->
            JsonObject(mapOf(
                "role" to JsonPrimitive(if (msg.role == "user") "user" else "assistant"),
                "content" to JsonPrimitive(msg.content)
            ))
        }

        val requestBody = buildJsonObject {
            put("model", modelName)
            put("max_tokens", 4096)
            putJsonArray("messages") { addAll(claudeMessages) }
            systemPrompt?.let { put("system", it) }
        }

        val request = Request.Builder()
            .url("$baseUrl/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Claude API错误 [${response.code}]: ${response.body?.string()}")
            }

            val body = response.body?.string() ?: ""
            val result = json.decodeFromString<ClaudeResponse>(body)
            val text = result.content?.firstOrNull()?.text ?: "无响应"
            emit(text)
        }
    }

    override suspend fun sendMessageSync(
        messages: List<ChatMessage>,
        systemPrompt: String?
    ): String {
        val sb = StringBuilder()
        sendMessage(messages, systemPrompt).collect { sb.append(it) }
        return sb.toString()
    }

    @Serializable
    data class ClaudeResponse(val content: List<ContentBlock>? = null)
    @Serializable
    data class ContentBlock(val type: String? = null, val text: String? = null)
}
