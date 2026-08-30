package com.assistant.ai

import com.assistant.ai.core.AgentTool
import com.assistant.ai.core.ToolCategory
import com.assistant.ai.core.ToolRegistry
import com.assistant.ai.core.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockTool(override val name: String) : AgentTool {
    override val description: String = "Mock test tool"
    override val category: ToolCategory = ToolCategory.SYSTEM

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val target = arguments["target"]?.toString() ?: "default"
        return ToolResult(true, "Executed mock tool on $target")
    }
}

class ToolRegistryTest {

    private lateinit var registry: ToolRegistry

    @Before
    fun setUp() {
        registry = ToolRegistry()
    }

    @Test
    fun testRegisterAndGetTool() {
        val tool = MockTool("testTool")
        registry.registerTool(tool)

        val retrieved = registry.getTool("TESTTOOL")
        assertEquals(tool, retrieved)
    }

    @Test
    fun testExecuteRegisteredTool() = runBlocking {
        registry.registerTool(MockTool("myAction"))

        val result = registry.executeTool("myAction", mapOf("target" to "screen"))
        assertTrue(result.success)
        assertEquals("Executed mock tool on screen", result.message)
    }

    @Test
    fun testExecuteUnregisteredToolReturnsError() = runBlocking {
        val result = registry.executeTool("unknownTool")
        assertFalse(result.success)
        assertTrue(result.message.contains("not registered"))
    }
}
