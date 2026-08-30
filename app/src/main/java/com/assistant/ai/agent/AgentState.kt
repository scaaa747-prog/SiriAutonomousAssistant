package com.assistant.ai.agent

sealed class AgentState {
    object Idle : AgentState()
    object Listening : AgentState()
    object Thinking : AgentState()
    data class Acting(val stepDescription: String) : AgentState()
    data class Speaking(val message: String) : AgentState()
    data class Error(val message: String) : AgentState()
    data class NeedsConfirmation(
        val actionDescription: String,
        val toolName: String,
        val arguments: Map<String, Any?>
    ) : AgentState()
}
