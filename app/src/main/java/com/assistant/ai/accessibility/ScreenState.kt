package com.assistant.ai.accessibility

data class ScreenRect(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    fun width(): Int = right - left
    fun height(): Int = bottom - top
    fun centerX(): Int = left + width() / 2
    fun centerY(): Int = top + height() / 2
}

data class UiElement(
    val id: String = "",
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val viewId: String? = null,
    val clickable: Boolean = false,
    val enabled: Boolean = true,
    val bounds: ScreenRect = ScreenRect(),
    val isFocused: Boolean = false,
    val isScrollable: Boolean = false
) {
    fun getLabel(): String {
        return text?.takeIf { it.isNotBlank() }
            ?: contentDescription?.takeIf { it.isNotBlank() }
            ?: viewId?.substringAfterLast(":id/")
            ?: className?.substringAfterLast(".")
            ?: "Unknown Element"
    }

    fun matchesQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return false
        return (text?.lowercase()?.contains(q) == true) ||
               (contentDescription?.lowercase()?.contains(q) == true) ||
               (viewId?.lowercase()?.contains(q) == true)
    }
}

data class ScreenState(
    val packageName: String = "",
    val activityName: String? = null,
    val elements: List<UiElement> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun findElementByText(text: String): UiElement? {
        val trimmed = text.lowercase().trim()
        return elements.firstOrNull { elem ->
            elem.text?.lowercase()?.contains(trimmed) == true ||
            elem.contentDescription?.lowercase()?.contains(trimmed) == true
        }
    }

    fun findElementById(viewId: String): UiElement? {
        return elements.firstOrNull { it.viewId == viewId || it.viewId?.endsWith(viewId) == true }
    }

    fun getClickableElements(): List<UiElement> {
        return elements.filter { it.clickable && it.enabled }
    }

    fun toReadableSummary(): String {
        if (elements.isEmpty()) return "App: $packageName (No visible UI elements)"
        val summary = StringBuilder("App: $packageName\nVisible UI Elements:\n")
        elements.take(30).forEachIndexed { index, elem ->
            val label = elem.getLabel()
            summary.append("- [$index] $label (clickable=${elem.clickable}, id=${elem.viewId ?: "none"})\n")
        }
        if (elements.size > 30) {
            summary.append("... and ${elements.size - 30} more elements.")
        }
        return summary.toString()
    }
}
