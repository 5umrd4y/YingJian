package com.yingjian.feature.photobook.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTemplateTest {
    @Test
    fun `slot ids are stable and ordered`() {
        assertEquals(listOf("slot-1"), PageTemplate.Single.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoHorizontal.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoVertical.slotIds)
        assertEquals(listOf("slot-1", "slot-2", "slot-3", "slot-4"), PageTemplate.GridFour.slotIds)
    }
}
