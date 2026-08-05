package com.aiworkgroup.data.api

import com.aiworkgroup.config.AIProvider
import com.aiworkgroup.config.AIProviderConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AIServiceFactory {

    fun createService(config: AIProviderConfig): AIService {
        val client = createOkHttpClient(config)

        return when (config.provider) {
            AIProvider.OPENAI -> OpenAIService(
                apiKey = config.apiKey,
                modelName = config.effectiveModel(),
                baseUrl = config.effectiveBaseUrl(),
                okHttpClient = client
            )
            AIProvider.GEMINI -> GeminiService(
                apiKey = config.apiKey,
                modelName = config.effectiveModel(),
                baseUrl = config.effectiveBaseUrl(),
                okHttpClient = client
            )
            AIProvider.CLAUDE -> ClaudeService(
                apiKey = config.apiKey,
                modelName = config.effectiveModel(),
                baseUrl = config.effectiveBaseUrl(),
                okHttpClient = client
            )
            AIProvider.DEEPSEEK -> DeepSeekService(
                apiKey = config.apiKey,
                modelName = config.effectiveModel(),
                baseUrl = config.effectiveBaseUrl(),
                okHttpClient = client
            )
            AIProvider.CUSTOM -> CustomAIService(
                apiKey = config.apiKey,
                modelName = config.effectiveModel(),
                baseUrl = config.effectiveBaseUrl(),
                okHttpClient = client
            )
        }
    }

    fun createServices(configs: List<AIProviderConfig>): Map<AIProvider, AIService> {
        return configs
            .filter { it.isEnabled && it.apiKey.isNotBlank() }
            .associate { it.provider to createService(it) }
    }

    private fun createOkHttpClient(config: AIProviderConfig): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
    }
}
