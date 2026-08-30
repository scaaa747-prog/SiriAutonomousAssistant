package com.assistant.ai

import com.assistant.ai.accessibility.ScreenRect
import com.assistant.ai.accessibility.ScreenState
import com.assistant.ai.accessibility.UiElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenObserverTest {

    @Test
    fun testScreenStateElementLookup() {
        val elem1 = UiElement(id = "e1", text = "Battery status", clickable = true, bounds = ScreenRect(0, 0, 100, 50))
        val elem2 = UiElement(id = "e2", contentDescription = "Wi-Fi Toggle", viewId = "com.android.settings:id/wifi", clickable = true, bounds = ScreenRect(0, 50, 100, 100))

        val state = ScreenState(packageName = "com.android.settings", elements = listOf(elem1, elem2))

        val foundText = state.findElementByText("battery")
        assertNotNull(foundText)
        assertEquals("Battery status", foundText?.text)

        val foundId = state.findElementById("wifi")
        assertNotNull(foundId)
        assertEquals("e2", foundId?.id)

        val notFound = state.findElementByText("nonexistent")
        assertNull(notFound)
    }

    @Test
    fun testUiElementQueryMatching() {
        val elem = UiElement(id = "e1", text = "Open Settings", viewId = "com.app:id/btn_settings", clickable = true)
        assertEquals(true, elem.matchesQuery("settings"))
        assertEquals(true, elem.matchesQuery("OPEN"))
        assertEquals(false, elem.matchesQuery("camera"))
    }
}
