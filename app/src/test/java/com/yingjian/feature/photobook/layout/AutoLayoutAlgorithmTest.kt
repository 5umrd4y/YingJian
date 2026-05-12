package com.yingjian.feature.photobook.layout

import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLayoutAlgorithmTest {

    @Test
    fun `layout creates one page per memory`() {
        val memories = listOf(
            testMemory(id = 1, width = 1200, height = 800),
            testMemory(id = 2, width = 800, height = 1200),
            testMemory(id = 3, width = 1000, height = 1000)
        )
        val photobook = PhotobookEntity(name = "Test", paperSize = "A4", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.A4, photobook)

        assertEquals(3, result.pages.size)
    }

    @Test
    fun `image fits within A4 safe area`() {
        val memories = listOf(testMemory(id = 1, width = 4000, height = 3000))
        val photobook = PhotobookEntity(name = "Test", paperSize = "A4", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.A4, photobook)

        val page = result.pages.first()
        val safeWidth = PaperSize.A4.widthMm - 6f // 3mm bleed each side
        val safeHeight = PaperSize.A4.heightMm - 6f

        val image = page.elements.first() as ImageElement
        assertTrue("Image width ${image.widthMm} exceeds safe $safeWidth", image.widthMm <= safeWidth + 0.1f)
        assertTrue("Image height ${image.heightMm} exceeds safe $safeHeight", image.heightMm <= safeHeight + 0.1f)
    }

    @Test
    fun `landscape photo maintains aspect ratio`() {
        val memories = listOf(testMemory(id = 1, width = 1200, height = 800)) // 3:2
        val photobook = PhotobookEntity(name = "Test", paperSize = "A4", createdAt = 0, updatedAt = 0)

        val result = AutoLayoutAlgorithm.layout(memories, PaperSize.A4, photobook)
        val image = result.pages.first().elements.first() as ImageElement

        val expectedRatio = 1200f / 800f
        val actualRatio = image.widthMm / image.heightMm
        assertEquals(expectedRatio, actualRatio, 0.01f)
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
