package com.yingjian.feature.photobook.export

import com.yingjian.feature.photobook.model.FitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfImageRenderMathTest {

    @Test
    fun `sample size keeps decoded dimensions within max bound`() {
        val sample = PdfImageRenderMath.calculateInSampleSize(
            sourceWidth = 12000,
            sourceHeight = 9000,
            targetWidth = 2200,
            targetHeight = 1500,
            maxDecodeDim = 4096
        )

        assertEquals(4, sample)
        assertTrue(12000 / sample <= 4096)
        assertTrue(9000 / sample <= 4096)
    }

    @Test
    fun `crop destination centers landscape image in square slot`() {
        val rect = PdfImageRenderMath.destinationRect(
            slotLeft = 10f,
            slotTop = 20f,
            slotWidth = 100f,
            slotHeight = 100f,
            imageWidth = 200,
            imageHeight = 100,
            fitMode = FitMode.Crop,
            cropScale = 1f,
            offsetX = 0f,
            offsetY = 0f
        )

        assertEquals(-40f, rect.left, 0.01f)
        assertEquals(20f, rect.top, 0.01f)
        assertEquals(160f, rect.right, 0.01f)
        assertEquals(120f, rect.bottom, 0.01f)
    }

    @Test
    fun `fit destination centers portrait image inside landscape slot`() {
        val rect = PdfImageRenderMath.destinationRect(
            slotLeft = 0f,
            slotTop = 0f,
            slotWidth = 120f,
            slotHeight = 80f,
            imageWidth = 100,
            imageHeight = 200,
            fitMode = FitMode.Fit,
            cropScale = 1f,
            offsetX = 0f,
            offsetY = 0f
        )

        assertEquals(40f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(80f, rect.right, 0.01f)
        assertEquals(80f, rect.bottom, 0.01f)
    }
}
