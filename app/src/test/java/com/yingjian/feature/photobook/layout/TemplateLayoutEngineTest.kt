package com.yingjian.feature.photobook.layout

import com.yingjian.feature.photobook.model.PageTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateLayoutEngineTest {
    @Test
    fun `single template returns one centered slot inside safe area`() {
        val slots = TemplateLayoutEngine.calculateSlots(
            LayoutInput(
                trimWidthMm = 285f,
                trimHeightMm = 210f,
                bleedMm = 3f,
                safeMarginMm = 16f,
                template = PageTemplate.Single
            )
        )

        assertEquals(1, slots.size)
        assertEquals("slot-1", slots.first().slotId)
        assertTrue(slots.first().xMm >= 16f)
        assertTrue(slots.first().yMm >= 16f)
        assertTrue(slots.first().xMm + slots.first().widthMm <= 285f - 16f)
        assertTrue(slots.first().yMm + slots.first().heightMm <= 210f - 28f)
    }

    @Test
    fun `grid four template returns four non-overlapping slots`() {
        val slots = TemplateLayoutEngine.calculateSlots(
            LayoutInput(285f, 210f, 3f, 16f, PageTemplate.GridFour)
        )

        assertEquals(listOf("slot-1", "slot-2", "slot-3", "slot-4"), slots.map { it.slotId })
        assertTrue(slots[0].xMm < slots[1].xMm)
        assertEquals(slots[0].yMm, slots[1].yMm, 0.001f)
        assertEquals(slots[2].yMm, slots[3].yMm, 0.001f)
        assertTrue(slots[2].yMm > slots[0].yMm)
    }
}
