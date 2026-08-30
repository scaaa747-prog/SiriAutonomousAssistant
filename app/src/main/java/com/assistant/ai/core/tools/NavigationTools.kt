package com.assistant.ai.core.tools

import com.assistant.ai.accessibility.AssistantAccessibilityService
import com.assistant.ai.android.SystemCommandExecutor
import com.assistant.ai.core.AgentTool
import com.assistant.ai.core.ToolCategory
import com.assistant.ai.core.ToolResult

class OpenAppTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "openApp"
    override val description: String = "Launches an application by name or package name. Arg: 'appName' or 'packageName'"
    override val category: ToolCategory = ToolCategory.NAVIGATION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val appName = arguments["appName"]?.toString()
            ?: arguments["packageName"]?.toString()
            ?: return ToolResult(false, "Missing required argument 'appName' or 'packageName'")

        val success = systemExecutor.openApp(appName)
        return if (success) {
            ToolResult(true, "Successfully opened application '$appName'")
        } else {
            ToolResult(false, "Failed to open application '$appName'. Verify it is installed.")
        }
    }
}

class OpenSettingsTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "openSettings"
    override val description: String = "Opens device settings or specific section like wifi, battery, bluetooth, display. Arg: 'section'"
    override val category: ToolCategory = ToolCategory.NAVIGATION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val section = arguments["section"]?.toString()
        val success = systemExecutor.openSettings(section)
        return if (success) {
            ToolResult(true, "Opened Settings section: ${section ?: "Main"}")
        } else {
            ToolResult(false, "Failed to open Settings")
        }
    }
}

class PressBackTool : AgentTool {
    override val name: String = "pressBack"
    override val description: String = "Triggers the system Back button"
    override val category: ToolCategory = ToolCategory.NAVIGATION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected. Enable it in Android Settings.")

        val success = service.performBack()
        return ToolResult(success, if (success) "Pressed Back" else "Failed to press Back")
    }
}

class PressHomeTool : AgentTool {
    override val name: String = "pressHome"
    override val description: String = "Triggers the system Home button"
    override val category: ToolCategory = ToolCategory.NAVIGATION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected.")

        val success = service.performHome()
        return ToolResult(success, if (success) "Pressed Home" else "Failed to press Home")
    }
}

class GetCurrentAppTool : AgentTool {
    override val name: String = "getCurrentApp"
    override val description: String = "Returns the active foreground application package name"
    override val category: ToolCategory = ToolCategory.NAVIGATION

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val service = AssistantAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility Service is not connected.")

        val state = service.captureCurrentScreen()
        return ToolResult(
            success = true,
            message = "Current application package: ${state.packageName}",
            data = mapOf("packageName" to state.packageName)
        )
    }
}
