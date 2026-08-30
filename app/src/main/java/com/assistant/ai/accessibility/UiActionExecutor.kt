package com.assistant.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class UiActionExecutor {

    companion object {
        private const val TAG = "UiActionExecutor"
    }

    fun performClick(service: AccessibilityService?, query: String): Boolean {
        val root = service?.rootInActiveWindow ?: return false
        val node = findMatchingNode(root, query)
        return if (node != null) {
            val result = executeClickOnNodeOrParent(node)
            node.recycle()
            result
        } else {
            Log.w(TAG, "No matching node found for click query: $query")
            false
        }
    }

    fun performLongClick(service: AccessibilityService?, query: String): Boolean {
        val root = service?.rootInActiveWindow ?: return false
        val node = findMatchingNode(root, query)
        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            node.recycle()
            result
        } else {
            Log.w(TAG, "No matching node found for long click query: $query")
            false
        }
    }

    fun typeText(service: AccessibilityService?, text: String, targetQuery: String? = null): Boolean {
        val root = service?.rootInActiveWindow ?: return false
        val node = if (!targetQuery.isNullOrBlank()) {
            findMatchingNode(root, targetQuery)
        } else {
            findFocusedOrEditableNode(root)
        }

        return if (node != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            node.recycle()
            result
        } else {
            Log.w(TAG, "No editable field found for typing text.")
            false
        }
    }

    fun scroll(service: AccessibilityService?, direction: String): Boolean {
        val root = service?.rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(root)
        val action = if (direction.equals("up", ignoreCase = true) || direction.equals("backward", ignoreCase = true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        return if (scrollableNode != null) {
            val result = scrollableNode.performAction(action)
            scrollableNode.recycle()
            result
        } else {
            Log.w(TAG, "No scrollable container found on screen.")
            false
        }
    }

    fun pressBack(service: AccessibilityService?): Boolean {
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) ?: false
    }

    fun pressHome(service: AccessibilityService?): Boolean {
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) ?: false
    }

    private fun findMatchingNode(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val q = query.lowercase().trim()
        val nodesByText = root.findAccessibilityNodeInfosByText(query)
        if (!nodesByText.isNullOrEmpty()) {
            return nodesByText.first()
        }

        // Deep search fallback
        return searchTree(root) { node ->
            val textMatch = node.text?.toString()?.lowercase()?.contains(q) == true
            val descMatch = node.contentDescription?.toString()?.lowercase()?.contains(q) == true
            val idMatch = node.viewIdResourceName?.lowercase()?.contains(q) == true
            textMatch || descMatch || idMatch
        }
    }

    private fun findFocusedOrEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) return focused

        return searchTree(root) { node ->
            node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true
        }
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return searchTree(root) { node -> node.isScrollable }
    }

    private fun executeClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isClickable) {
                return curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            curr = curr.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun searchTree(
        node: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val match = searchTree(child, predicate)
            if (match != null) return match
        }
        return null
    }
}
