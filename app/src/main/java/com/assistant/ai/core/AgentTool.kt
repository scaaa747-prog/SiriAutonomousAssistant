package com.assistant.ai.core

enum class ToolCategory {
    SYSTEM,
    NAVIGATION,
    INTERACTION,
    MEDIA
}

data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap()
)

interface AgentTool {
    val name: String
    val description: String
    val category: ToolCategory
    val isSensitive: Boolean get() = false

    suspend fun execute(arguments: Map<String, Any?>): ToolResult
}
