package com.yingjian.feature.photobook

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotobookPreviewLayoutTest {

    @Test
    fun `spread fits width constrained landscape viewport`() {
        val size = PhotobookPreviewLayout.fitSpread(1200f, 600f)

        assertEquals(1200f, size.spreadWidthDp, 0.01f)
        assertEquals(442.11f, size.spreadHeightDp, 0.01f)
        assertEquals(600f, size.pageWidthDp, 0.01f)
        assertEquals(442.11f, size.pageHeightDp, 0.01f)
    }

    @Test
    fun `spread fits height constrained landscape viewport`() {
        val size = PhotobookPreviewLayout.fitSpread(1200f, 300f)

        assertEquals(814.29f, size.spreadWidthDp, 0.01f)
        assertEquals(300f, size.spreadHeightDp, 0.01f)
        assertEquals(407.14f, size.pageWidthDp, 0.01f)
        assertEquals(300f, size.pageHeightDp, 0.01f)
    }

    @Test
    fun `single page preserves page aspect ratio`() {
        val size = PhotobookPreviewLayout.fitSinglePage(400f, 700f)

        assertEquals(400f, size.pageWidthDp, 0.01f)
        assertEquals(294.74f, size.pageHeightDp, 0.01f)
    }
}
