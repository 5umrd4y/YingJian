package com.yingjian.feature.photobook.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverLayoutTest {
    @Test
    fun `default cover layout stays inside page bounds`() {
        val layout = CoverLayoutDefaults.defaultCover(title = "My Book", subtitle = "2026")

        assertEquals(CoverPageType.Cover, layout.pageType)
        assertEquals("#AAA194", layout.backgroundColor)
        assertTrue(layout.textElements.any { it.id == "cover-title" && it.role == CoverTextRole.Title })
        layout.textElements.forEach { element ->
            assertTrue(element.xMm >= 0f)
            assertTrue(element.yMm >= 0f)
            assertTrue(element.xMm + element.widthMm <= 285f)
            assertTrue(element.yMm + element.heightMm <= 210f)
        }
    }

    @Test
    fun `alignment maps to compose and pdf values`() {
        assertEquals(androidx.compose.ui.text.style.TextAlign.Start, PhotobookTextAlign.Start.toComposeTextAlign())
        assertEquals(android.graphics.Paint.Align.CENTER, PhotobookTextAlign.Center.toPaintAlign())
        assertEquals(android.text.Layout.Alignment.ALIGN_OPPOSITE, PhotobookTextAlign.End.toStaticLayoutAlignment())
    }

    @Test
    fun `update text changes only target element`() {
        val layout = CoverLayoutDefaults.defaultCover(title = "Old", subtitle = "Sub")

        val updated = layout.updateText("cover-title", "New")

        assertEquals("New", updated.textElements.first { it.id == "cover-title" }.text)
        assertEquals("Sub", updated.textElements.first { it.id == "cover-subtitle" }.text)
    }
}
