package com.aiworkgroup.domain

import com.aiworkgroup.config.AIProvider
import com.aiworkgroup.config.AIRole
import com.aiworkgroup.data.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AIOrchestrator(
    private val services: Map<AIProvider, AIService>
) {

    suspend fun createWorkGroup(taskDescription: String): WorkGroup {
        val mainService = services[AIProvider.OPENAI]
            ?: services[AIProvider.GEMINI]
            ?: services[AIProvider.CLAUDE]
            ?: services[AIProvider.DEEPSEEK]
            ?: services.values.first()

        val prompt = """
            请分析以下任务，并设计一个高效的工作组来完成它。

            任务描述: $taskDescription

            请按以下格式输出（严格JSON格式，不要markdown标记）：
            {
                "groupName": "工作组名称",
                "description": "工作组目标描述",
                "roles": [
                    {
                        "name": "角色名称",
                        "description": "角色职责描述",
                        "systemPrompt": "该角色的系统提示词，定义其行为和专业领域",
                        "provider": "OPENAI 或 GEMINI 或 CLAUDE",
                        "avatar": "emoji"
                    }
                ]
            }

            要求：
            1. 角色数量2-5个，覆盖任务所需的所有专业领域
            2. 每个角色要有明确的职责边界
            3. 根据角色特点分配最适合的AI提供商
            4. 角色之间要有互补性
        """.trimIndent()

        val response = mainService.sendMessageSync(
            listOf(ChatMessage("user", prompt))
        )

        return parseWorkGroup(response, taskDescription)
    }

    suspend fun assignTasks(workGroup: WorkGroup, taskDescription: String): Map<String, String> {
        val assignments = mutableMapOf<String, String>()

        coroutineScope {
            workGroup.roles.map { role ->
                async {
                    val service = services[role.serviceProvider] ?: services.values.first()

                    val prompt = """
                        你是"${role.name}"，职责是：${role.description}

                        整体任务: $taskDescription

                        请明确你在这个任务中的具体工作内容和交付物：
                        1. 你的核心任务是什么
                        2. 你需要产出什么成果
                        3. 你需要与其他哪些角色协作
                        4. 你的工作标准和质量要求

                        请用简洁清晰的中文回答。
                    """.trimIndent()

                    val result = service.sendMessageSync(
                        listOf(ChatMessage("user", prompt)),
                        role.systemPrompt
                    )

                    role.id to result
                }
            }.awaitAll().forEach { (roleId, result) ->
                assignments[roleId] = result
            }
        }

        return assignments
    }

    fun startDiscussion(
        workGroup: WorkGroup,
        taskDescription: String,
        topic: String,
        rounds: Int = 3
    ): Flow<DiscussionMessage> = flow {
        val discussionHistory = mutableListOf<ChatMessage>()
        val initialMessage = ChatMessage("user", "讨论主题: $topic
整体任务: $taskDescription")
        discussionHistory.add(initialMessage)

        emit(DiscussionMessage("system", "💬 讨论开始", "system", "🔔"))

        repeat(rounds) { round ->
            emit(DiscussionMessage("system", "--- 第 ${round + 1} 轮讨论 ---", "system", "📌"))

            workGroup.roles.forEach { role ->
                val service = services[role.serviceProvider] ?: services.values.first()

                val context = buildString {
                    appendLine("你是${role.name}，${role.description}")
                    appendLine("
之前的讨论内容：")
                    discussionHistory.forEach { msg ->
                        appendLine("${msg.role}: ${msg.content}")
                    }
                    appendLine("
请基于你的专业角度发表意见，保持简洁（100字以内）。")
                }

                val response = service.sendMessageSync(
                    listOf(ChatMessage("user", context)),
                    role.systemPrompt
                )

                val msg = DiscussionMessage(role.id, response, role.name, role.avatar)
                discussionHistory.add(ChatMessage("assistant", "${role.name}: $response"))
                emit(msg)

                delay(500)
            }
        }

        emit(DiscussionMessage("system", "
✅ 讨论结束，正在生成总结...", "system", "🔔"))
    }

    suspend fun generateFinalPlan(
        workGroup: WorkGroup,
        taskDescription: String,
        assignments: Map<String, String>,
        discussionHistory: List<DiscussionMessage>
    ): String {
        val mainService = services[AIProvider.OPENAI]
            ?: services[AIProvider.GEMINI]
            ?: services.values.first()

        val prompt = buildString {
            appendLine("基于以下信息，生成最终的工作方案：")
            appendLine("
任务: $taskDescription")
            appendLine("
分工情况:")
            assignments.forEach { (roleId, task) ->
                val role = workGroup.roles.find { it.id == roleId }
                appendLine("- ${role?.name}: $task")
            }
            appendLine("
讨论记录:")
            discussionHistory.filter { it.roleId != "system" }.forEach { msg ->
                appendLine("${msg.roleName}: ${msg.content}")
            }
            appendLine("
请生成一份结构化的最终方案，包括：")
            appendLine("1. 执行计划（分阶段）")
            appendLine("2. 各角色具体职责")
            appendLine("3. 协作流程")
            appendLine("4. 交付物清单")
            appendLine("5. 风险评估")
        }

        return mainService.sendMessageSync(listOf(ChatMessage("user", prompt)))
    }

    private fun parseWorkGroup(response: String, taskDescription: String): WorkGroup {
        val roles = mutableListOf<AIRole>()

        val rolePattern = """"name"\s*:\s*"([^"]+)"""".toRegex()
        val descPattern = """"description"\s*:\s*"([^"]+)"""".toRegex()
        val promptPattern = """"systemPrompt"\s*:\s*"([^"]+)"""".toRegex()

        val names = rolePattern.findAll(response).map { it.groupValues[1] }.toList()
        val descriptions = descPattern.findAll(response).map { it.groupValues[1] }.toList()
        val prompts = promptPattern.findAll(response).map { it.groupValues[1] }.toList()

        names.forEachIndexed { index, name ->
            roles.add(AIRole(
                id = "role_$index",
                name = name,
                description = descriptions.getOrElse(index) { "负责$name相关工作" },
                systemPrompt = prompts.getOrElse(index) { "你是$name，专业且高效" },
                serviceProvider = when (index % 3) {
                    0 -> AIProvider.OPENAI
                    1 -> AIProvider.GEMINI
                    else -> AIProvider.CLAUDE
                },
                avatar = listOf("👨‍💼", "👩‍🔬", "👨‍🎨", "👩‍💻", "👨‍🏫").getOrElse(index) { "🤖" }
            ))
        }

        if (roles.isEmpty()) {
            roles.addAll(listOf(
                AIRole("planner", "策划师", "负责整体规划和方案设计",
                    "你是资深项目策划师，擅长分析需求并制定执行方案", AIProvider.OPENAI, "📋"),
                AIRole("developer", "技术专家", "负责技术实现方案",
                    "你是技术专家，擅长评估技术可行性和实现方案", AIProvider.GEMINI, "⚙️"),
                AIRole("reviewer", "质量审核", "负责审核和优化",
                    "你是质量审核专家，擅长发现问题并提出改进建议", AIProvider.CLAUDE, "🔍")
            ))
        }

        return WorkGroup(
            id = System.currentTimeMillis().toString(),
            name = "AI工作组-${System.currentTimeMillis()}",
            description = taskDescription,
            roles = roles,
            createdAt = System.currentTimeMillis()
        )
    }
}

data class WorkGroup(
    val id: String,
    val name: String,
    val description: String,
    val roles: List<AIRole>,
    val createdAt: Long
)

data class DiscussionMessage(
    val roleId: String,
    val content: String,
    val roleName: String,
    val avatar: String
)
