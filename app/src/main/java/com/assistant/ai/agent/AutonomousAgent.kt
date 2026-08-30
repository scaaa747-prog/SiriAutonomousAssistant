package com.assistant.ai.agent

import android.util.Log
import com.assistant.ai.accessibility.AssistantAccessibilityService
import com.assistant.ai.core.ToolRegistry
import com.assistant.ai.data.ActionResult
import com.assistant.ai.data.ConversationContext
import com.assistant.ai.data.MessageRole
import com.assistant.ai.data.SettingsRepository
import com.assistant.ai.model.AgentContext
import com.assistant.ai.model.AgentDecision
import com.assistant.ai.model.ModelManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutonomousAgent(
    private val toolRegistry: ToolRegistry,
    private val modelManager: ModelManager,
    private val conversationContext: ConversationContext,
    private val settingsRepository: SettingsRepository
) {

    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private val maxStepsLimit = 10
    private var isCancelled = false

    suspend fun executeTask(userRequest: String): String {
        isCancelled = false
        _agentState.value = AgentState.Thinking
        conversationContext.addMessage(MessageRole.USER, userRequest)
        conversationContext.startTask(userRequest)

        val actionHistory = mutableListOf<String>()
        var stepCount = 0

        try {
            while (stepCount < maxStepsLimit && !isCancelled) {
                stepCount++
                Log.d(TAG, "Starting Agent Loop Step $stepCount for user request: '$userRequest'")

                // 1. Observe Screen
                val screenState = AssistantAccessibilityService.instance?.captureCurrentScreen()

                // 2. Build Context
                val agentContext = AgentContext(
                    userQuery = userRequest,
                    screenState = screenState,
                    conversationSummary = conversationContext.toSummaryPrompt(),
                    availableTools = toolRegistry.getToolsDescription()
                )

                // 3. Understand & Plan (Model decision)
                val isLowEnd = settingsRepository.settingsState.value.lowEndDeviceOptimization
                val decision = modelManager.getDecision(userRequest, agentContext, isLowEnd)

                when (decision) {
                    is AgentDecision.TaskComplete -> {
                        val response = decision.resultMessage
                        conversationContext.addMessage(MessageRole.ASSISTANT, response)
                        conversationContext.updateTaskProgress(stepCount, completed = true)
                        _agentState.value = AgentState.Speaking(response)
                        return response
                    }

                    is AgentDecision.AskClarification -> {
                        val question = decision.question
                        conversationContext.addMessage(MessageRole.ASSISTANT, question)
                        _agentState.value = AgentState.Speaking(question)
                        return question
                    }

                    is AgentDecision.NeedConfirmation -> {
                        _agentState.value = AgentState.NeedsConfirmation(
                            actionDescription = decision.actionDescription,
                            toolName = decision.pendingToolName,
                            arguments = decision.arguments
                        )
                        return decision.actionDescription
                    }

                    is AgentDecision.ToolCall -> {
                        val toolSignature = "${decision.toolName}:${decision.arguments}"

                        // Loop detection guardrail
                        if (actionHistory.count { it == toolSignature } >= 3) {
                            val failureMsg = "Loop detected: Repeated action '$toolSignature' 3 times without screen progression."
                            Log.w(TAG, failureMsg)
                            conversationContext.addMessage(MessageRole.ASSISTANT, failureMsg)
                            _agentState.value = AgentState.Error(failureMsg)
                            return failureMsg
                        }
                        actionHistory.add(toolSignature)

                        // Check sensitive action setting
                        val tool = toolRegistry.getTool(decision.toolName)
                        val confirmRequired = settingsRepository.settingsState.value.confirmSensitiveActions
                        if (tool != null && tool.isSensitive && confirmRequired) {
                            _agentState.value = AgentState.NeedsConfirmation(
                                actionDescription = "Execute sensitive action: ${tool.name} with ${decision.arguments}",
                                toolName = tool.name,
                                arguments = decision.arguments
                            )
                            return "Action requires confirmation"
                        }

                        // 4. Act (Execute Tool)
                        _agentState.value = AgentState.Acting("Step $stepCount: ${decision.explanation}")

                        val result = toolRegistry.executeTool(decision.toolName, decision.arguments)
                        conversationContext.addActionResult(
                            ActionResult(
                                toolName = decision.toolName,
                                arguments = decision.arguments,
                                success = result.success,
                                output = result.message
                            )
                        )

                        // Direct System Tools Auto-Completion (Instant <10ms execution without loop)
                        val singleStepTools = setOf(
                            "getbattery", "getvolume", "setvolume", "getbrightness",
                            "setbrightness", "pressback", "presshome", "playmedia", "pausemedia"
                        )
                        if (singleStepTools.contains(decision.toolName.lowercase()) && result.success) {
                            val response = result.message
                            conversationContext.addMessage(MessageRole.ASSISTANT, response)
                            conversationContext.updateTaskProgress(stepCount, completed = true)
                            _agentState.value = AgentState.Speaking(response)
                            return response
                        }

                        // 5. Observe Again & Verify State
                        delay(400)
                        AssistantAccessibilityService.instance?.captureCurrentScreen()

                        if (!result.success) {
                            Log.w(TAG, "Step $stepCount tool failed: ${result.message}")
                        }
                    }
                }
            }

            if (stepCount >= maxStepsLimit) {
                val limitMsg = "Reached maximum step limit ($maxStepsLimit) for this task."
                _agentState.value = AgentState.Error(limitMsg)
                return limitMsg
            }

            val finishMsg = "Task finished."
            _agentState.value = AgentState.Speaking(finishMsg)
            return finishMsg

        } catch (e: CancellationException) {
            _agentState.value = AgentState.Idle
            return "Task cancelled."
        } catch (e: Exception) {
            Log.e(TAG, "Error in Agent loop execution", e)
            val errorMsg = "An error occurred while executing the task: ${e.localizedMessage}"
            _agentState.value = AgentState.Error(errorMsg)
            return errorMsg
        }
    }

    suspend fun continueConfirmedAction(toolName: String, arguments: Map<String, Any?>): String {
        _agentState.value = AgentState.Acting("Executing confirmed action: $toolName")
        val result = toolRegistry.executeTool(toolName, arguments)
        conversationContext.addActionResult(
            ActionResult(
                toolName = toolName,
                arguments = arguments,
                success = result.success,
                output = result.message
            )
        )
        val msg = if (result.success) "Successfully executed confirmed action." else "Failed to execute action."
        _agentState.value = AgentState.Speaking(msg)
        return msg
    }

    fun cancelTask() {
        isCancelled = true
        _agentState.value = AgentState.Idle
    }

    companion object {
        private const val TAG = "AutonomousAgent"
    }
}
