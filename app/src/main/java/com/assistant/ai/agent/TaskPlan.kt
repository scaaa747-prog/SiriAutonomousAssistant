package com.assistant.ai.agent

data class TaskStep(
    val stepIndex: Int,
    val actionDescription: String,
    val toolName: String,
    val arguments: Map<String, Any?>,
    var status: StepStatus = StepStatus.PENDING,
    var resultMessage: String? = null
)

enum class StepStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED
}

data class TaskPlan(
    val goal: String,
    val steps: MutableList<TaskStep> = mutableListOf(),
    var isFinished: Boolean = false,
    var success: Boolean = false,
    var finalResponse: String = ""
)
