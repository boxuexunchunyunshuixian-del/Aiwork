package com.aiworkgroup.config

/**
 * AI 角色定义
 */
data class AIRole(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val serviceProvider: AIProvider,
    val avatar: String = "🤖"
)
