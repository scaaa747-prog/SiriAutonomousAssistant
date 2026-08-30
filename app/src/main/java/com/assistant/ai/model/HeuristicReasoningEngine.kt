package com.assistant.ai.model

import android.util.Log

class HeuristicReasoningEngine : LocalReasoningEngine {

    override var isModelLoaded: Boolean = true

    override suspend fun loadModel(): Boolean {
        isModelLoaded = true
        return true
    }

    override suspend fun unloadModel() {
        // Fast path engine requires negligible RAM
    }

    override suspend fun generate(context: AgentContext): AgentDecision {
        val query = context.userQuery.lowercase().trim()
        val screen = context.screenState

        Log.d(TAG, "Evaluating query: '$query' (Current app: ${screen?.packageName})")

        // 1. Direct System Commands (Deterministic Fast-Path)
        if (query.contains("battery")) {
            return AgentDecision.ToolCall("getBattery", emptyMap(), "Checking device battery status")
        }

        if (query.contains("volume")) {
            val num = extractNumber(query)
            return if (num != null) {
                AgentDecision.ToolCall("setVolume", mapOf("percentage" to num), "Setting volume to $num%")
            } else if (query.contains("up") || query.contains("increase")) {
                AgentDecision.ToolCall("setVolume", mapOf("percentage" to 80), "Increasing media volume")
            } else if (query.contains("down") || query.contains("decrease") || query.contains("lower")) {
                AgentDecision.ToolCall("setVolume", mapOf("percentage" to 30), "Lowering media volume")
            } else {
                AgentDecision.ToolCall("getVolume", emptyMap(), "Getting volume level")
            }
        }

        if (query.contains("brightness")) {
            val num = extractNumber(query)
            return if (num != null) {
                AgentDecision.ToolCall("setBrightness", mapOf("percentage" to num), "Setting brightness to $num%")
            } else {
                AgentDecision.ToolCall("getBrightness", emptyMap(), "Getting brightness level")
            }
        }

        if (query.equals("press back", ignoreCase = true) || query.equals("go back", ignoreCase = true)) {
            return AgentDecision.ToolCall("pressBack", emptyMap(), "Going back")
        }

        if (query.equals("go home", ignoreCase = true) || query.equals("press home", ignoreCase = true)) {
            return AgentDecision.ToolCall("pressHome", emptyMap(), "Going home")
        }

        // 2. Open Settings or Apps
        if (query.contains("open settings") || query.contains("go to settings")) {
            val isSettingsOpen = screen?.packageName?.contains("settings", ignoreCase = true) == true
            if (!isSettingsOpen) {
                val section = when {
                    query.contains("wifi") || query.contains("wi-fi") -> "wifi"
                    query.contains("battery") -> "battery"
                    query.contains("bluetooth") -> "bluetooth"
                    query.contains("display") || query.contains("brightness") -> "display"
                    else -> null
                }
                return AgentDecision.ToolCall("openSettings", mapOf("section" to section), "Opening Settings app")
            }
        }

        if (query.startsWith("open ") || query.startsWith("launch ")) {
            val targetApp = query.removePrefix("open ").removePrefix("launch ").trim()
            if (targetApp.isNotEmpty() && !query.contains("settings")) {
                return AgentDecision.ToolCall("openApp", mapOf("appName" to targetApp), "Opening app $targetApp")
            }
        }

        // 3. Multi-Step Screen Reasoning if already inside target app
        if (screen != null && screen.elements.isNotEmpty()) {
            // Wi-Fi multi-step goal handling
            if (query.contains("wifi") || query.contains("wi-fi")) {
                val wifiElem = screen.findElementByText("wi-fi") ?: screen.findElementByText("network")
                if (wifiElem != null && wifiElem.clickable) {
                    return AgentDecision.ToolCall("tapElement", mapOf("query" to wifiElem.getLabel()), "Tapping Wi-Fi setting item")
                }
                val savedNetwork = screen.findElementByText("connected") ?: screen.findElementByText("saved")
                if (savedNetwork != null) {
                    return AgentDecision.TaskComplete("Successfully navigated to Wi-Fi settings and verified status.")
                }
            }

            // Battery multi-step goal handling
            if (query.contains("battery")) {
                val batteryElem = screen.findElementByText("battery")
                if (batteryElem != null && batteryElem.clickable) {
                    return AgentDecision.ToolCall("tapElement", mapOf("query" to batteryElem.getLabel()), "Tapping Battery setting item")
                }
            }

            // Generic search match on screen elements
            val keywords = query.split(" ").filter { it.length > 3 }
            for (kw in keywords) {
                val match = screen.findElementByText(kw)
                if (match != null && match.clickable) {
                    return AgentDecision.ToolCall("tapElement", mapOf("query" to match.getLabel()), "Tapping screen element matching '$kw'")
                }
            }
        }

        // Fallback default decision
        return AgentDecision.TaskComplete("Task processed for prompt: '${context.userQuery}'")
    }

    private fun extractNumber(input: String): Int? {
        val regex = Regex("\\b\\d+\\b")
        val match = regex.find(input)
        return match?.value?.toIntOrNull()?.coerceIn(0, 100)
    }

    companion object {
        private const val TAG = "HeuristicReasoningEngine"
    }
}
