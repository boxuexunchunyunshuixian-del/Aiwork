package com.aiworkgroup.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiworkgroup.config.AIProviderConfig
import com.aiworkgroup.config.APIConfigManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val configManager = APIConfigManager(application)

    private val _configs = MutableStateFlow<List<AIProviderConfig>>(emptyList())
    val configs: StateFlow<List<AIProviderConfig>> = _configs.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        viewModelScope.launch {
            configManager.configsFlow.collect { configs ->
                _configs.value = configs
            }
        }
    }

    fun updateConfig(config: AIProviderConfig) {
        _configs.update { list ->
            list.map { if (it.provider == config.provider) config else it }
        }
        _isSaved.value = false
    }

    fun saveConfigs() {
        viewModelScope.launch {
            configManager.saveConfigs(_configs.value)
            _isSaved.value = true
        }
    }
}
