package com.aiworkgroup.data.api

import kotlinx.coroutines.flow.Flow

interface AIService {
    val providerName: String
    val modelName: String

    fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Flow<String>

    suspend fun sendMessageSync(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): String
}

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface ConfigurableAIService : AIService {
    val config: com.aiworkgroup.config.AIProviderConfig
}
