package com.yingjian.feature.photobook

import com.yingjian.core.data.database.MemoryRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotobookPhotoOrderingTest {
    @Test
    fun `sorts selected photos by memory timestamp ascending`() {
        val photos = listOf(
            photo(memoryId = 2),
            photo(memoryId = 1),
            photo(memoryId = 3)
        )
        val memoryById = mapOf(
            1L to memory(id = 1, timestamp = 2000),
            2L to memory(id = 2, timestamp = 1000),
            3L to memory(id = 3, timestamp = 3000)
        )

        val ordered = PhotobookPhotoOrdering.byMemoryDateAscending(photos, memoryById)

        assertEquals(listOf(2L, 1L, 3L), ordered.map { it.memoryId })
    }

    @Test
    fun `keeps selected order for multiple photos from same memory`() {
        val photos = listOf(
            photo(memoryId = 1, sourceImageIndex = 1),
            photo(memoryId = 1, sourceImageIndex = 0)
        )
        val memoryById = mapOf(1L to memory(id = 1, timestamp = 1000))

        val ordered = PhotobookPhotoOrdering.byMemoryDateAscending(photos, memoryById)

        assertEquals(listOf(1, 0), ordered.map { it.sourceImageIndex })
    }

    private fun photo(memoryId: Long, sourceImageIndex: Int = 0) = SelectedMemoryPhoto(
        memoryId = memoryId,
        imageUri = "content://image/$memoryId/$sourceImageIndex",
        sourceImageIndex = sourceImageIndex
    )

    private fun memory(id: Long, timestamp: Long) = MemoryRecordEntity(
        id = id,
        imageUri = "content://image/$id",
        imageWidth = 1,
        imageHeight = 1,
        timestamp = timestamp,
        latitude = null,
        longitude = null,
        moodText = null,
        tags = "",
        createdAt = timestamp
    )
}
