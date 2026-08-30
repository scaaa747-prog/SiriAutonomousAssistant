package com.assistant.ai.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicInteger

class ScreenObserver {

    private val idCounter = AtomicInteger(0)

    fun parseScreenState(
        rootNode: AccessibilityNodeInfo?,
        packageName: String = ""
    ): ScreenState {
        if (rootNode == null) {
            return ScreenState(packageName = packageName, elements = emptyList())
        }

        idCounter.set(0)
        val elements = mutableListOf<UiElement>()
        traverseNode(rootNode, elements)

        val pkg = rootNode.packageName?.toString() ?: packageName
        return ScreenState(
            packageName = pkg,
            elements = elements
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        elements: MutableList<UiElement>
    ) {
        if (node == null || !node.isVisibleToUser) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val screenRect = ScreenRect(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom
        )

        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        val viewId = node.viewIdResourceName
        val className = node.className?.toString()

        val isMeaningful = !text.isNullOrBlank() ||
                !contentDesc.isNullOrBlank() ||
                !viewId.isNullOrBlank() ||
                node.isClickable ||
                node.isCheckable ||
                node.isScrollable

        if (isMeaningful) {
            val element = UiElement(
                id = "elem_${idCounter.getAndIncrement()}",
                text = text,
                contentDescription = contentDesc,
                className = className,
                viewId = viewId,
                clickable = node.isClickable,
                enabled = node.isEnabled,
                bounds = screenRect,
                isFocused = node.isFocused,
                isScrollable = node.isScrollable
            )
            elements.add(element)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, elements)
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }
}
