package com.assistant.ai.data

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class Message(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActionResult(
    val toolName: String,
    val arguments: Map<String, Any?>,
    val success: Boolean,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Task(
    val id: String,
    val userGoal: String,
    val stepsExecuted: Int = 0,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false
)

class ConversationContext(private val maxMessages: Int = 10) {

    private val _messages = mutableListOf<Message>()
    val messages: List<Message> get() = _messages.toList()

    private val _recentActions = mutableListOf<ActionResult>()
    val recentActions: List<ActionResult> get() = _recentActions.toList()

    var currentTask: Task? = null

    fun addMessage(role: MessageRole, content: String) {
        _messages.add(Message(role, content))
        if (_messages.size > maxMessages) {
            _messages.removeAt(0)
        }
    }

    fun addActionResult(actionResult: ActionResult) {
        _recentActions.add(actionResult)
        if (_recentActions.size > 5) {
            _recentActions.removeAt(0)
        }
    }

    fun startTask(userGoal: String): Task {
        val task = Task(
            id = "task_${System.currentTimeMillis()}",
            userGoal = userGoal
        )
        currentTask = task
        return task
    }

    fun updateTaskProgress(stepsCount: Int, completed: Boolean = false, failed: Boolean = false) {
        currentTask?.let {
            currentTask = it.copy(
                stepsExecuted = stepsCount,
                isCompleted = completed,
                isFailed = failed
            )
        }
    }

    fun clearHistory() {
        _messages.clear()
        _recentActions.clear()
        currentTask = null
    }

    fun toSummaryPrompt(): String {
        val sb = StringBuilder()
        if (_messages.isNotEmpty()) {
            sb.append("Recent Conversation History:\n")
            _messages.takeLast(6).forEach { msg ->
                sb.append("${msg.role}: ${msg.content}\n")
            }
        }
        if (_recentActions.isNotEmpty()) {
            sb.append("\nRecent Actions Executed:\n")
            _recentActions.takeLast(3).forEach { act ->
                sb.append("- ${act.toolName}(${act.arguments}): ${if (act.success) "SUCCESS" else "FAILED"} -> ${act.output}\n")
            }
        }
        return sb.toString()
    }
}
