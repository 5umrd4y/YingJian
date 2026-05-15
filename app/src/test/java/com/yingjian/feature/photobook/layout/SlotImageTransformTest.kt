package com.yingjian.feature.photobook.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class SlotImageTransformTest {
    @Test
    fun `clamps crop scale to minimum one`() {
        val result = SlotImageTransform.clampScale(0.5f)
        assertEquals(1f, result, 0.001f)
    }

    @Test
    fun `clamps crop scale to maximum three`() {
        val result = SlotImageTransform.clampScale(4.2f)
        assertEquals(3f, result, 0.001f)
    }
}
