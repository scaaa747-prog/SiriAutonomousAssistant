package com.assistant.ai

import com.assistant.ai.accessibility.ScreenRect
import com.assistant.ai.accessibility.ScreenState
import com.assistant.ai.accessibility.UiElement
import com.assistant.ai.model.AgentContext
import com.assistant.ai.model.AgentDecision
import com.assistant.ai.model.HeuristicReasoningEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeuristicReasoningEngineTest {

    private lateinit var engine: HeuristicReasoningEngine

    @Before
    fun setUp() {
        engine = HeuristicReasoningEngine()
    }

    @Test
    fun testBatteryStatusIntent() = runBlocking {
        val context = AgentContext(userQuery = "check my battery status")
        val decision = engine.generate(context)

        assertTrue(decision is AgentDecision.ToolCall)
        val toolCall = decision as AgentDecision.ToolCall
        assertEquals("getBattery", toolCall.toolName)
    }

    @Test
    fun testSetVolumeIntent() = runBlocking {
        val context = AgentContext(userQuery = "set volume to 75%")
        val decision = engine.generate(context)

        assertTrue(decision is AgentDecision.ToolCall)
        val toolCall = decision as AgentDecision.ToolCall
        assertEquals("setVolume", toolCall.toolName)
        assertEquals(75, toolCall.arguments["percentage"])
    }

    @Test
    fun testOpenSettingsIntent() = runBlocking {
        val context = AgentContext(userQuery = "go to wifi settings")
        val decision = engine.generate(context)

        assertTrue(decision is AgentDecision.ToolCall)
        val toolCall = decision as AgentDecision.ToolCall
        assertEquals("openSettings", toolCall.toolName)
        assertEquals("wifi", toolCall.arguments["section"])
    }

    @Test
    fun testMultiStepUiElementMatch() = runBlocking {
        val elements = listOf(
            UiElement(id = "1", text = "Wi-Fi", clickable = true, bounds = ScreenRect(0, 0, 100, 50)),
            UiElement(id = "2", text = "Bluetooth", clickable = true, bounds = ScreenRect(0, 50, 100, 100))
        )
        val screenState = ScreenState(packageName = "com.android.settings", elements = elements)
        val context = AgentContext(userQuery = "connect to wi-fi", screenState = screenState)

        val decision = engine.generate(context)
        assertTrue(decision is AgentDecision.ToolCall)
        val toolCall = decision as AgentDecision.ToolCall
        assertEquals("tapElement", toolCall.toolName)
    }
}
