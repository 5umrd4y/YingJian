package com.yingjian.feature.photobook.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverLayoutTest {
    @Test
    fun `default cover layout stays inside page bounds`() {
        val layout = CoverLayoutDefaults.defaultCover(title = "My Book", subtitle = "2026")

        assertEquals(CoverPageType.Cover, layout.pageType)
        assertEquals("#AAA194", layout.backgroundColor)
        assertEquals(listOf(CoverTextRole.Title), layout.textElements.map { it.role })
        val title = layout.textElements.single()
        assertEquals("My Book", title.text)
        assertEquals(142.5f, title.xMm + title.widthMm / 2f, 0.001f)
        assertEquals(105f, title.yMm + title.heightMm / 2f, 0.001f)
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
    }

    @Test
    fun `default back cover is blank`() {
        val layout = CoverLayoutDefaults.defaultBackCover(
            title = "Back",
            subtitle = "Subtitle",
            dateText = "2026.05.16"
        )

        assertEquals(CoverPageType.BackCover, layout.pageType)
        assertEquals("#AAA194", layout.backgroundColor)
        assertTrue(layout.textElements.isEmpty())
        assertFalse(layout.textElements.any { it.role == CoverTextRole.Divider })
    }
}
