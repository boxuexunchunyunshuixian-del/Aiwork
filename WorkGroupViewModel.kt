package com.aiworkgroup.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiworkgroup.config.AIProvider
import com.aiworkgroup.config.APIConfigManager
import com.aiworkgroup.data.api.*
import com.aiworkgroup.domain.AIOrchestrator
import com.aiworkgroup.domain.DiscussionMessage
import com.aiworkgroup.domain.WorkGroup
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkGroupViewModel(application: Application) : AndroidViewModel(application) {

    private val configManager = APIConfigManager(application)

    private val _services = MutableStateFlow<Map<AIProvider, AIService>>(emptyMap())

    private val _uiState = MutableStateFlow(WorkGroupUiState())
    val uiState: StateFlow<WorkGroupUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _discussionMessages = MutableStateFlow<List<DiscussionMessage>>(emptyList())
    val discussionMessages: StateFlow<List<DiscussionMessage>> = _discussionMessages.asStateFlow()

    val hasAvailableService: Flow<Boolean> = configManager.enabledConfigsFlow
        .map { it.isNotEmpty() }

    init {
        viewModelScope.launch {
            configManager.enabledConfigsFlow.collect { configs ->
                _services.value = AIServiceFactory.createServices(configs)
            }
        }
    }

    fun createWorkGroup(taskDescription: String) {
        if (_services.value.isEmpty()) {
            _uiState.update { it.copy(error = "请先配置至少一个AI的API密钥") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val updatedOrchestrator = AIOrchestrator(_services.value)
                val workGroup = updatedOrchestrator.createWorkGroup(taskDescription)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        workGroup = workGroup,
                        currentStep = WorkFlowStep.ASSIGN_TASKS
                    )
                }

                addMessage(ChatMessage(
                    "assistant",
                    "✅ 工作组「${workGroup.name}」已创建！

成员：
" +
                    workGroup.roles.joinToString("
") {
                        "${it.avatar} ${it.name} (${it.serviceProvider}) - ${it.description}"
                    }
                ))

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun assignTasks() {
        val workGroup = _uiState.value.workGroup ?: return
        val taskDesc = workGroup.description

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val updatedOrchestrator = AIOrchestrator(_services.value)
                val assignments = updatedOrchestrator.assignTasks(workGroup, taskDesc)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        assignments = assignments,
                        currentStep = WorkFlowStep.DISCUSSION
                    )
                }

                val assignmentText = assignments.map { (roleId, task) ->
                    val role = workGroup.roles.find { it.id == roleId }
                    "${role?.avatar} **${role?.name}**
$task"
                }.joinToString("

")

                addMessage(ChatMessage("assistant", "📋 分工完成：

$assignmentText"))

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun startDiscussion(topic: String, rounds: Int = 3) {
        val workGroup = _uiState.value.workGroup ?: return
        val taskDesc = workGroup.description

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isDiscussing = true) }
            _discussionMessages.value = emptyList()

            try {
                val updatedOrchestrator = AIOrchestrator(_services.value)
                updatedOrchestrator.startDiscussion(workGroup, taskDesc, topic, rounds)
                    .collect { msg ->
                        _discussionMessages.update { it + msg }
                        _uiState.update { it.copy(isLoading = false) }
                    }

                _uiState.update {
                    it.copy(isDiscussing = false, currentStep = WorkFlowStep.FINAL_PLAN)
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isDiscussing = false, error = e.message)
                }
            }
        }
    }

    fun generateFinalPlan() {
        val workGroup = _uiState.value.workGroup ?: return
        val assignments = _uiState.value.assignments
        val discussionHistory = _discussionMessages.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val updatedOrchestrator = AIOrchestrator(_services.value)
                val plan = updatedOrchestrator.generateFinalPlan(
                    workGroup, workGroup.description, assignments, discussionHistory
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        finalPlan = plan,
                        currentStep = WorkFlowStep.COMPLETED
                    )
                }

                addMessage(ChatMessage("assistant", "📄 最终方案：

$plan"))

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendUserMessage(content: String) {
        addMessage(ChatMessage("user", content))

        when (_uiState.value.currentStep) {
            WorkFlowStep.IDLE -> createWorkGroup(content)
            WorkFlowStep.ASSIGN_TASKS -> assignTasks()
            WorkFlowStep.DISCUSSION -> {
                val service = _services.value.values.firstOrNull() ?: return
                viewModelScope.launch {
                    val response = service.sendMessageSync(
                        listOf(ChatMessage("user", content)),
                        "你是协调员，帮助用户与AI工作组互动"
                    )
                    addMessage(ChatMessage("assistant", response))
                }
            }
            else -> {}
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.update { it + message }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        _uiState.value = WorkGroupUiState()
        _messages.value = emptyList()
        _discussionMessages.value = emptyList()
    }
}

data class WorkGroupUiState(
    val isLoading: Boolean = false,
    val isDiscussing: Boolean = false,
    val workGroup: WorkGroup? = null,
    val assignments: Map<String, String> = emptyMap(),
    val finalPlan: String = "",
    val currentStep: WorkFlowStep = WorkFlowStep.IDLE,
    val error: String? = null
)

enum class WorkFlowStep {
    IDLE, ASSIGN_TASKS, DISCUSSION, FINAL_PLAN, COMPLETED
}
