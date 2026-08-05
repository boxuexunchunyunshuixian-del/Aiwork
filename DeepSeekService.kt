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

class DeepSeekService(
    private val apiKey: String,
    override val modelName: String = "deepseek-chat",
    private val baseUrl: String = "https://api.deepseek.com/v1",
    okHttpClient: OkHttpClient = OkHttpClient()
) : AIService, ConfigurableAIService {

    override val providerName: String = "DeepSeek"
    override val config: AIProviderConfig = AIProviderConfig(
        provider = AIProvider.DEEPSEEK,
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
        val requestBody = buildJsonObject {
            put("model", modelName)
            put("stream", true)
            putJsonArray("messages") {
                systemPrompt?.let {
                    addJsonObject {
                        put("role", "system")
                        put("content", it)
                    }
                }
                messages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    }
                }
            }
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("DeepSeek API错误 [${response.code}]: ${response.body?.string()}")
            }

            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        if (data == "[DONE]") break
                        try {
                            val chunk = json.decodeFromString<DSChunk>(data)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (!content.isNullOrEmpty()) emit(content)
                        } catch (_: Exception) {}
                    }
                }
            }
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
    data class DSChunk(val choices: List<DSChoice>)
    @Serializable
    data class DSChoice(val delta: DSDelta)
    @Serializable
    data class DSDelta(val content: String? = null)
}
