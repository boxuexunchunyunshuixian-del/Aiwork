package com.aiworkgroup.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.apiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "api_configs")

@Serializable
data class AIProviderConfig(
    val provider: AIProvider,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val isEnabled: Boolean = true,
    val timeoutSeconds: Int = 60,
    val maxTokens: Int = 4096
) {
    val displayName: String get() = when(provider) {
        AIProvider.OPENAI -> "OpenAI"
        AIProvider.GEMINI -> "Google Gemini"
        AIProvider.CLAUDE -> "Anthropic Claude"
        AIProvider.DEEPSEEK -> "DeepSeek"
        AIProvider.CUSTOM -> "自定义"
    }

    val defaultModel: String get() = when(provider) {
        AIProvider.OPENAI -> "gpt-4o"
        AIProvider.GEMINI -> "gemini-2.0-flash"
        AIProvider.CLAUDE -> "claude-3-sonnet-20240229"
        AIProvider.DEEPSEEK -> "deepseek-chat"
        AIProvider.CUSTOM -> ""
    }

    val defaultBaseUrl: String get() = when(provider) {
        AIProvider.OPENAI -> "https://api.openai.com/v1"
        AIProvider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
        AIProvider.CLAUDE -> "https://api.anthropic.com/v1"
        AIProvider.DEEPSEEK -> "https://api.deepseek.com/v1"
        AIProvider.CUSTOM -> ""
    }

    fun effectiveModel(): String = modelName.ifBlank { defaultModel }
    fun effectiveBaseUrl(): String = baseUrl.ifBlank { defaultBaseUrl }
}

enum class AIProvider {
    OPENAI, GEMINI, CLAUDE, DEEPSEEK, CUSTOM
}

class APIConfigManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val CONFIGS_KEY = stringPreferencesKey("ai_provider_configs")
        private val DEFAULT_CONFIGS = listOf(
            AIProviderConfig(AIProvider.OPENAI),
            AIProviderConfig(AIProvider.GEMINI),
            AIProviderConfig(AIProvider.CLAUDE),
            AIProviderConfig(AIProvider.DEEPSEEK),
            AIProviderConfig(AIProvider.CUSTOM)
        )
    }

    val configsFlow: Flow<List<AIProviderConfig>> = context.apiConfigDataStore.data
        .map { preferences ->
            val jsonStr = preferences[CONFIGS_KEY]
            if (jsonStr != null) {
                try {
                    json.decodeFromString<List<AIProviderConfig>>(jsonStr)
                } catch (_: Exception) {
                    DEFAULT_CONFIGS
                }
            } else {
                DEFAULT_CONFIGS
            }
        }

    val enabledConfigsFlow: Flow<List<AIProviderConfig>> = configsFlow
        .map { configs -> configs.filter { it.isEnabled && it.apiKey.isNotBlank() } }

    suspend fun saveConfigs(configs: List<AIProviderConfig>) {
        context.apiConfigDataStore.edit { preferences ->
            preferences[CONFIGS_KEY] = json.encodeToString(configs)
        }
    }
}
