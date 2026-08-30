package com.assistant.ai.core.tools

import com.assistant.ai.accessibility.AssistantAccessibilityService
import com.assistant.ai.core.AgentTool
import com.assistant.ai.core.ToolCategory
import com.assistant.ai.core.ToolResult

class TapElementTool : AgentTool {
    override val name: String = "tapElement"
    override val description: String = "Taps/clicks a UI element on screen by text, description, or id. Arg: 'query'"
    override val category: ToolCategory = ToolCategory.INTERACTION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val query = arguments["query"]?.toString()
            ?: arguments["text"]?.toString()
            ?: return ToolResult(false, "Missing required argument 'query'")

        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected.")

        val success = service.performClick(query)
        return if (success) {
            ToolResult(true, "Tapped element matching '$query'")
        } else {
            ToolResult(false, "Could not find or tap element matching '$query' on the current screen.")
        }
    }
}

class LongPressElementTool : AgentTool {
    override val name: String = "longPressElement"
    override val description: String = "Long presses a UI element on screen by text or query. Arg: 'query'"
    override val category: ToolCategory = ToolCategory.INTERACTION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val query = arguments["query"]?.toString()
            ?: return ToolResult(false, "Missing required argument 'query'")

        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected.")

        val success = service.performLongClick(query)
        return ToolResult(success, if (success) "Long pressed '$query'" else "Failed to long press '$query'")
    }
}

class ScrollTool : AgentTool {
    override val name: String = "scroll"
    override val description: String = "Scrolls the screen content. Arg: 'direction' ('down', 'up', 'forward', 'backward')"
    override val category: ToolCategory = ToolCategory.INTERACTION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val direction = arguments["direction"]?.toString() ?: "down"
        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected.")

        val success = service.performScroll(direction)
        return ToolResult(success, if (success) "Scrolled screen $direction" else "Could not scroll screen")
    }
}

class TypeTextTool : AgentTool {
    override val name: String = "typeText"
    override val description: String = "Enters text into an editable field on screen. Args: 'text', optional 'targetQuery'"
    override val category: ToolCategory = ToolCategory.INTERACTION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val text = arguments["text"]?.toString()
            ?: return ToolResult(false, "Missing required argument 'text'")
        val targetQuery = arguments["targetQuery"]?.toString()

        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected.")

        val success = service.performType(text, targetQuery)
        return if (success) {
            ToolResult(true, "Typed text: '$text'")
        } else {
            ToolResult(false, "Failed to enter text into target field.")
        }
    }
}

class ReadScreenTool : AgentTool {
    override val name: String = "readScreen"
    override val description: String = "Observes and extracts current visible screen hierarchy elements"
    override val category: ToolCategory = ToolCategory.INTERACTION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected.")

        val screenState = service.captureCurrentScreen()
        val summary = screenState.toReadableSummary()
        return ToolResult(
            success = true,
            message = summary,
            data = mapOf(
                "packageName" to screenState.packageName,
                "elementCount" to screenState.elements.size
            )
        )
    }
}
