package com.assistant.ai.core

import android.util.Log

class ToolRegistry {

    private val tools = mutableMapOf<String, AgentTool>()

    fun registerTool(tool: AgentTool) {
        tools[tool.name.lowercase()] = tool
        Log.d(TAG, "Registered tool: ${tool.name}")
    }

    fun getTool(name: String): AgentTool? {
        return tools[name.lowercase()]
    }

    fun getAllTools(): List<AgentTool> {
        return tools.values.toList()
    }

    suspend fun executeTool(name: String, arguments: Map<String, Any?> = emptyMap()): ToolResult {
        val tool = getTool(name)
            ?: return ToolResult(
                success = false,
                message = "Tool '$name' is not registered in the system tool registry."
            )

        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing tool: $name", e)
            ToolResult(
                success = false,
                message = "Execution error in tool '$name': ${e.localizedMessage}"
            )
        }
    }

    fun getToolsDescription(): String {
        val sb = StringBuilder("Available Tools:\n")
        tools.values.forEach { tool ->
            sb.append("- ${tool.name}: ${tool.description}\n")
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "ToolRegistry"
    }
}
