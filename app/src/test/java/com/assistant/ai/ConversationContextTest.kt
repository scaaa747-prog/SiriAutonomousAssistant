package com.assistant.ai

import com.assistant.ai.data.ActionResult
import com.assistant.ai.data.ConversationContext
import com.assistant.ai.data.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ConversationContextTest {

    private lateinit var context: ConversationContext

    @Before
    fun setUp() {
        context = ConversationContext(maxMessages = 3)
    }

    @Test
    fun testBoundedMessageHistory() {
        context.addMessage(MessageRole.USER, "Msg 1")
        context.addMessage(MessageRole.ASSISTANT, "Msg 2")
        context.addMessage(MessageRole.USER, "Msg 3")
        context.addMessage(MessageRole.ASSISTANT, "Msg 4")

        assertEquals(3, context.messages.size)
        assertEquals("Msg 2", context.messages[0].content)
        assertEquals("Msg 4", context.messages[2].content)
    }

    @Test
    fun testTaskTracking() {
        val task = context.startTask("Open settings and check battery")
        assertNotNull(task)
        assertEquals("Open settings and check battery", context.currentTask?.userGoal)

        context.updateTaskProgress(2, completed = true)
        assertEquals(true, context.currentTask?.isCompleted)
        assertEquals(2, context.currentTask?.stepsExecuted)
    }
}
