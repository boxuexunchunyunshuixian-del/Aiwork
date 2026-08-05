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

class GeminiService(
    private val apiKey: String,
    override val modelName: String = "gemini-2.0-flash",
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    okHttpClient: OkHttpClient = OkHttpClient()
) : AIService, ConfigurableAIService {

    override val providerName: String = "Gemini"
    override val config: AIProviderConfig = AIProviderConfig(
        provider = AIProvider.GEMINI,
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
        val contents = messages.map { msg ->
            JsonObject(mapOf(
                "role" to JsonPrimitive(if (msg.role == "assistant") "model" else "user"),
                "parts" to JsonArray(listOf(
                    JsonObject(mapOf("text" to JsonPrimitive(msg.content)))
                ))
            ))
        }

        val requestBody = buildJsonObject {
            putJsonArray("contents") { addAll(contents) }
            systemPrompt?.let {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", it) }
                    }
                }
            }
        }

        val request = Request.Builder()
            .url("$baseUrl/models/$modelName:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Gemini API错误 [${response.code}]: ${response.body?.string()}")
            }

            val body = response.body?.string() ?: ""
            val result = json.decodeFromString<GeminiResponse>(body)
            val text = result.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text ?: "无响应"
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
    data class GeminiResponse(val candidates: List<Candidate>? = null)
    @Serializable
    data class Candidate(val content: Content? = null)
    @Serializable
    data class Content(val parts: List<Part>? = null)
    @Serializable
    data class Part(val text: String? = null)
}
