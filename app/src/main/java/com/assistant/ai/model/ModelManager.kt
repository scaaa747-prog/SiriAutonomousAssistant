package com.assistant.ai.model

import android.content.Context
import android.util.Log

class ModelManager(private val context: Context) {

    private val fastPathEngine = HeuristicReasoningEngine()
    private val llmEngine = QuantizedLlmEngine(context)

    suspend fun getDecision(
        userQuery: String,
        agentContext: AgentContext,
        isLowEndMode: Boolean = true
    ): AgentDecision {
        // Low-End Device Optimization: Check fast-path deterministic engine first
        val fastDecision = fastPathEngine.generate(agentContext)

        if (isLowEndMode || fastDecision !is AgentDecision.TaskComplete || fastDecision.resultMessage.contains("Task processed")) {
            if (fastDecision !is AgentDecision.TaskComplete) {
                return fastDecision
            }
        }

        // If complex reasoning is required and LLM is enabled:
        Log.d(TAG, "Routing request to quantized local LLM engine...")
        val llmDecision = llmEngine.generate(agentContext)

        // Automatically unload heavy model after execution when idle
        if (isLowEndMode) {
            llmEngine.unloadModel()
        }

        return llmDecision
    }

    suspend fun releaseResources() {
        llmEngine.unloadModel()
    }

    companion object {
        private const val TAG = "ModelManager"
    }
}
