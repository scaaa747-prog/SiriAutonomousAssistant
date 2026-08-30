package com.assistant.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantAccessibilityService : AccessibilityService() {

    private val screenObserver = ScreenObserver()
    private val actionExecutor = UiActionExecutor()

    private val _currentScreenState = MutableStateFlow(ScreenState())
    val currentScreenState: StateFlow<ScreenState> = _currentScreenState.asStateFlow()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.i(TAG, "AssistantAccessibilityService connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val pkgName = event.packageName?.toString() ?: ""
                val newState = screenObserver.parseScreenState(rootInActiveWindow, pkgName)
                _currentScreenState.value = newState
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AssistantAccessibilityService interrupted.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        _isServiceActive.value = false
        Log.i(TAG, "AssistantAccessibilityService unbound.")
        return super.onUnbind(intent)
    }

    fun captureCurrentScreen(): ScreenState {
        val root = rootInActiveWindow
        val pkg = root?.packageName?.toString() ?: _currentScreenState.value.packageName
        val state = screenObserver.parseScreenState(root, pkg)
        _currentScreenState.value = state
        return state
    }

    fun performClick(query: String): Boolean {
        return actionExecutor.performClick(this, query)
    }

    fun performLongClick(query: String): Boolean {
        return actionExecutor.performLongClick(this, query)
    }

    fun performType(text: String, query: String? = null): Boolean {
        return actionExecutor.typeText(this, text, query)
    }

    fun performScroll(direction: String): Boolean {
        return actionExecutor.scroll(this, direction)
    }

    fun performBack(): Boolean {
        return actionExecutor.pressBack(this)
    }

    fun performHome(): Boolean {
        return actionExecutor.pressHome(this)
    }

    companion object {
        private const val TAG = "AssistantAccessibility"

        @Volatile
        var instance: AssistantAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun isConnected(): Boolean = instance != null
    }
}
