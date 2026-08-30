package com.assistant.ai.model

import com.assistant.ai.accessibility.ScreenState

data class AgentContext(
    val userQuery: String,
    val screenState: ScreenState? = null,
    val conversationSummary: String = "",
    val availableTools: String = ""
)

sealed class AgentDecision {
    data class ToolCall(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val explanation: String = ""
    ) : AgentDecision()

    data class TaskComplete(
        val resultMessage: String
    ) : AgentDecision()

    data class NeedConfirmation(
        val actionDescription: String,
        val pendingToolName: String,
        val arguments: Map<String, Any?>
    ) : AgentDecision()

    data class AskClarification(
        val question: String
    ) : AgentDecision()
}

interface LocalReasoningEngine {
    val isModelLoaded: Boolean
    suspend fun loadModel(): Boolean
    suspend fun unloadModel()
    suspend fun generate(context: AgentContext): AgentDecision
}
