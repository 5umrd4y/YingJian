package com.yingjian.feature.photobook.layout

import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateLayoutEngineTest {
    private val input = LayoutInput(
        trimWidthMm = 285f,
        trimHeightMm = 210f,
        bleedMm = 3f,
        safeMarginMm = 16f,
        bottomTextReserveMm = 20f,
        gutterMm = 8f,
        template = PageTemplate.SingleLandscape
    )

    @Test
    fun `single landscape is centered with 3 to 2 ratio`() {
        val slot = TemplateLayoutEngine.calculateSlots(input).single()

        val contentWidth = 285f - 16f * 2f
        val contentHeight = 210f - 16f * 2f - 20f
        val expectedWidth = minOf(contentWidth, contentHeight * 1.5f)
        val expectedHeight = expectedWidth / 1.5f

        assertEquals("slot-1", slot.slotId)
        assertEquals(expectedWidth, slot.widthMm, 0.01f)
        assertEquals(expectedHeight, slot.heightMm, 0.01f)
        assertEquals(16f + (contentWidth - expectedWidth) / 2f, slot.xMm, 0.01f)
        assertEquals(16f + (contentHeight - expectedHeight) / 2f, slot.yMm, 0.01f)
    }

    @Test
    fun `single portrait is centered with 2 to 3 ratio`() {
        val slot = TemplateLayoutEngine.calculateSlots(input.copy(template = PageTemplate.SinglePortrait)).single()

        val contentWidth = 285f - 16f * 2f
        val contentHeight = 210f - 16f * 2f - 20f
        val expectedHeight = minOf(contentHeight, contentWidth * 1.5f)
        val expectedWidth = expectedHeight * (2f / 3f)

        assertEquals("slot-1", slot.slotId)
        assertEquals(expectedWidth, slot.widthMm, 0.01f)
        assertEquals(expectedHeight, slot.heightMm, 0.01f)
        assertEquals(16f + (contentWidth - expectedWidth) / 2f, slot.xMm, 0.01f)
        assertEquals(16f + (contentHeight - expectedHeight) / 2f, slot.yMm, 0.01f)
    }

    @Test
    fun `all template slots stay inside content area`() {
        PageTemplate.entries.forEach { template ->
            val slots = TemplateLayoutEngine.calculateSlots(input.copy(template = template))
            slots.forEach { slot ->
                assertTrue("${template.name} ${slot.slotId} left", slot.xMm >= PhotobookLayoutDefaults.SAFE_MARGIN_MM)
                assertTrue("${template.name} ${slot.slotId} top", slot.yMm >= PhotobookLayoutDefaults.SAFE_MARGIN_MM)
                assertTrue("${template.name} ${slot.slotId} right", slot.xMm + slot.widthMm <= 285f - 16f)
                assertTrue("${template.name} ${slot.slotId} bottom", slot.yMm + slot.heightMm <= 210f - 16f - 20f)
            }
        }
    }
}
