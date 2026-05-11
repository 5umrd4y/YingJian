package com.yingjian.core.util

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class UnitConverterTest {

    @Test
    fun `mmToPx300Dpi converts 25 point 4mm to 300px`() {
        assertEquals(300, 25.4f.mmToPx300Dpi())
    }

    @Test
    fun `px300DpiToMm converts 300px to 25 point 4mm`() {
        assertEquals(25.4f, 300.px300DpiToMm(), 0.01f)
    }

    @Test
    fun `Dp to Mm on 160dpi device`() {
        val density = Density(density = 1f)
        val result = Dp(1f).toMm(density, densityDpi = 160)
        assertEquals(25.4f / 160, result, 0.01f)
    }

    @Test
    fun `Dp to 300DPI Px on 160dpi 100dp chain conversion`() {
        val density = Density(density = 1f)
        val result = Dp(100f).toPx300Dpi(density, densityDpi = 160)
        // 100dp -> 100px(screen) -> 100 * 25.4/160 mm -> (that mm) * 300/25.4 px = 100 * 300/160 = 187.5 -> rounds to 188
        val expected = (100f * 300 / 160).roundToInt()
        assertEquals(expected, result)
    }
}
