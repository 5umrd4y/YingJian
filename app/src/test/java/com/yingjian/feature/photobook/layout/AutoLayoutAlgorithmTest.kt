package com.yingjian.feature.photobook.layout

import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLayoutAlgorithmTest {

    @Test
    fun `layout creates single template page per memory`() {
        val memories = listOf(
            testMemory(id = 1, width = 1200, height = 800),
            testMemory(id = 2, width = 800, height = 1200)
        )
        val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)

        assertEquals(2, result.pages.size)
        assertEquals(PageTemplate.SingleLandscape, result.pages[0].template)
        assertEquals("content://test/1", result.pages[0].slots.first().imageRef?.imageUri)
        assertEquals(1f, result.pages[0].slots.first().cropScale, 0.001f)
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
