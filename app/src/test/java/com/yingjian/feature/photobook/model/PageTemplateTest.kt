package com.yingjian.feature.photobook.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTemplateTest {
    @Test
    fun `templates contain five stable entries`() {
        assertEquals(
            listOf(
                PageTemplate.SingleLandscape,
                PageTemplate.SinglePortrait,
                PageTemplate.TwoHorizontal,
                PageTemplate.TwoVertical,
                PageTemplate.GridFour
            ),
            PageTemplate.entries
        )
    }

    @Test
    fun `template slot ids are stable`() {
        assertEquals(listOf("slot-1"), PageTemplate.SingleLandscape.slotIds)
        assertEquals(listOf("slot-1"), PageTemplate.SinglePortrait.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoHorizontal.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoVertical.slotIds)
        assertEquals(listOf("slot-1", "slot-2", "slot-3", "slot-4"), PageTemplate.GridFour.slotIds)
    }

    @Test
    fun `template capacities match slot count`() {
        PageTemplate.entries.forEach { template ->
            assertEquals(template.slotIds.size, template.capacity)
        }
    }
}
