package com.yingjian.feature.photobook.export

import com.yingjian.feature.photobook.model.FitMode
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.TextElement
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class PdfPageFooterTest {
    @Test
    fun `footer uses same font size for mood date and page number`() {
        assertEquals(PdfPageFooter.TEXT_FONT_SIZE_MM, PdfPageFooter.PAGE_NUMBER_FONT_SIZE_MM)
        assertEquals(4f, PdfPageFooter.TEXT_FONT_SIZE_MM)
    }

    @Test
    fun `footer resolves date from first non empty slot memory`() {
        val page = PageState(
            pageNumber = 2,
            template = PageTemplate.GridFour,
            slots = listOf(
                ImageSlot(slotId = "slot-1", imageRef = null),
                ImageSlot(
                    slotId = "slot-2",
                    imageRef = ImageRef(
                        memoryId = 42L,
                        imageUri = "content://image/42",
                        sourceImageIndex = 0
                    ),
                    fitMode = FitMode.Fit
                )
            ),
            textElements = listOf(TextElement(text = "好心情", xMm = 0f, yMm = 0f, widthMm = 10f, heightMm = 4f, zIndex = 1)),
            trimWidthMm = 285f,
            trimHeightMm = 210f
        )

        val footer = PdfPageFooter.fromPage(
            page = page,
            memoryTimestamps = mapOf(42L to 1_767_225_600_000L),
            zoneId = ZoneOffset.UTC
        )

        assertEquals("好心情", footer.moodText)
        assertEquals("2026.01.01", footer.dateText)
        assertEquals("2", footer.pageNumberText)
    }
}
