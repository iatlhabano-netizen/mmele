package bw.mmele

import bw.mmele.ui.board.pointDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTest {
    @Test fun pointLabelsReadNaturally() {
        assertEquals("Point A, empty, valid move", pointDescription("A", null, selected = false, target = true, shootable = false, movable = false))
        assertEquals("Point d5, Obsidian cow, selected", pointDescription("d5", "Obsidian", selected = true, target = false, shootable = false, movable = false))
        assertEquals("Point a1, Quartz cow, can be shot", pointDescription("a1", "Quartz", selected = false, target = false, shootable = true, movable = false))
        assertEquals("Point g7, Obsidian cow, can move", pointDescription("g7", "Obsidian", selected = false, target = false, shootable = false, movable = true))
    }

    @Test fun contactAndLinksAreInOnePlace() {
        assertTrue(AppInfo.PRIVACY_URL.startsWith("https://"))
        assertTrue(AppInfo.TERMS_URL.startsWith("https://"))
        assertTrue(AppInfo.CONTACT_EMAIL.contains("@"))
    }
}
