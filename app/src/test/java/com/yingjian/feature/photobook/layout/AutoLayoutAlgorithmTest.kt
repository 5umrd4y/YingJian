package com.yingjian.feature.photobook.layout

import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLayoutAlgorithmTest {

    @Test
    fun `layout chooses landscape single for landscape memory`() {
        val memories = listOf(testMemory(id = 1, width = 1200, height = 800))
        val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)

        assertEquals(PageTemplate.SingleLandscape, result.pages.single().template)
        assertEquals(1200, result.pages.single().slots.single().imageRef?.imageWidth)
        assertEquals(800, result.pages.single().slots.single().imageRef?.imageHeight)
    }

    @Test
    fun `layout chooses portrait single for portrait memory`() {
        val memories = listOf(testMemory(id = 2, width = 800, height = 1200))
        val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)

        assertEquals(PageTemplate.SinglePortrait, result.pages.single().template)
    }

    @Test
    fun `create single photo page falls back to landscape when dimensions are unknown`() {
        val page = AutoLayoutAlgorithm.createSinglePhotoPage(
            imageRef = ImageRef(memoryId = 1, imageUri = "content://test/1", sourceImageIndex = 0),
            moodText = null,
            paperSize = PaperSize.TWELVE_INCH_LANDSCAPE,
            pageNumber = 1
        )

        assertEquals(PageTemplate.SingleLandscape, page.template)
    }

    @Test
    fun `page has correct trim dimensions`() {
        val memories = listOf(testMemory(id = 1, width = 4000, height = 3000))
        val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)

        val page = result.pages.first()
        assertEquals(PaperSize.TWELVE_INCH_LANDSCAPE.widthMm, page.trimWidthMm)
        assertEquals(PaperSize.TWELVE_INCH_LANDSCAPE.heightMm, page.trimHeightMm)
    }

    @Test
    fun `page includes mood text when present`() {
        val memories = listOf(testMemory(id = 1, width = 1200, height = 800))
        val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)
        val page = result.pages.first()

        assertTrue(page.textElements.isNotEmpty())
        assertEquals("Test mood", page.textElements.first().text)
        assertEquals(PaperSize.TWELVE_INCH_LANDSCAPE.widthMm * 0.15f, page.textElements.first().xMm, 0.01f)
        assertEquals(PaperSize.TWELVE_INCH_LANDSCAPE.heightMm - 22f, page.textElements.first().yMm, 0.01f)
    }

    @Test
    fun `slot has valid image ref`() {
        val memories = listOf(testMemory(id = 1, width = 1200, height = 800))
        val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)
        val slot = result.pages.first().slots.first()

        assertNotNull(slot.imageRef)
        assertEquals(1L, slot.imageRef?.memoryId)
        assertEquals(0, slot.imageRef?.sourceImageIndex)
    }

    private fun testMemory(id: Long, width: Int, height: Int) = MemoryRecordEntity(
        id = id,
        imageUri = "content://test/$id",
        imageWidth = width,
        imageHeight = height,
        timestamp = System.currentTimeMillis(),
        latitude = null,
        longitude = null,
        moodText = "Test mood",
        tags = "[]",
        createdAt = System.currentTimeMillis()
    )
}
