package com.assistant.ai.core.tools

import com.assistant.ai.android.SystemCommandExecutor
import com.assistant.ai.core.AgentTool
import com.assistant.ai.core.ToolCategory
import com.assistant.ai.core.ToolResult

class SetVolumeTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "setVolume"
    override val description: String = "Sets media volume level. Arg: 'percentage' (0-100)"
    override val category: ToolCategory = ToolCategory.SYSTEM

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val levelStr = arguments["percentage"]?.toString()
            ?: arguments["level"]?.toString()
            ?: return ToolResult(false, "Missing required argument 'percentage'")

        val level = levelStr.toDoubleOrNull()?.toInt() ?: 50
        val success = systemExecutor.setVolume(level)
        return ToolResult(success, if (success) "Volume set to $level%" else "Failed to adjust volume")
    }
}

class GetVolumeTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "getVolume"
    override val description: String = "Gets current media volume percentage"
    override val category: ToolCategory = ToolCategory.SYSTEM

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val volume = systemExecutor.getVolume()
        return ToolResult(true, "Current media volume is $volume%", mapOf("volume" to volume))
    }
}

class SetBrightnessTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "setBrightness"
    override val description: String = "Sets screen brightness level. Arg: 'percentage' (0-100)"
    override val category: ToolCategory = ToolCategory.SYSTEM

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val levelStr = arguments["percentage"]?.toString()
            ?: return ToolResult(false, "Missing required argument 'percentage'")

        val level = levelStr.toDoubleOrNull()?.toInt() ?: 50
        val success = systemExecutor.setBrightness(level)
        return ToolResult(success, if (success) "Brightness set to $level%" else "Failed to change brightness")
    }
}

class GetBrightnessTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "getBrightness"
    override val description: String = "Gets current display brightness percentage"
    override val category: ToolCategory = ToolCategory.SYSTEM

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val brightness = systemExecutor.getBrightness()
        return ToolResult(true, "Current screen brightness is $brightness%", mapOf("brightness" to brightness))
    }
}

class GetBatteryTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "getBattery"
    override val description: String = "Gets current battery percentage and charging status"
    override val category: ToolCategory = ToolCategory.SYSTEM

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val level = systemExecutor.getBatteryLevel()
        val isCharging = systemExecutor.isCharging()
        val statusText = if (isCharging) "charging" else "discharging"
        return ToolResult(
            success = true,
            message = "Battery status: $level% ($statusText)",
            data = mapOf("battery" to level, "isCharging" to isCharging)
        )
    }
}

class PlayMediaTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "playMedia"
    override val description: String = "Sends media play playback key event"
    override val category: ToolCategory = ToolCategory.MEDIA

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val success = systemExecutor.playMedia()
        return ToolResult(success, if (success) "Media playback started" else "Failed to play media")
    }
}

class PauseMediaTool(private val systemExecutor: SystemCommandExecutor) : AgentTool {
    override val name: String = "pauseMedia"
    override val description: String = "Sends media pause playback key event"
    override val category: ToolCategory = ToolCategory.MEDIA

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val success = systemExecutor.pauseMedia()
        return ToolResult(success, if (success) "Media playback paused" else "Failed to pause media")
    }
}
