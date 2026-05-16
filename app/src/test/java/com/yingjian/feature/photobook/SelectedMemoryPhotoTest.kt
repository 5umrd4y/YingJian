package com.yingjian.feature.photobook

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedMemoryPhotoTest {

    @Test
    fun `with image dimensions replaces stale memory dimensions`() {
        val photo = SelectedMemoryPhoto(
            memoryId = 1,
            imageUri = "content://test/portrait",
            sourceImageIndex = 1,
            imageWidth = 1200,
            imageHeight = 800
        )

        val resolved = photo.withImageDimensions(width = 800, height = 1200)

        assertEquals(800, resolved.imageWidth)
        assertEquals(1200, resolved.imageHeight)
    }

    @Test
    fun `with image dimensions keeps original dimensions when resolved size is invalid`() {
        val photo = SelectedMemoryPhoto(
            memoryId = 1,
            imageUri = "content://test/portrait",
            sourceImageIndex = 1,
            imageWidth = 1200,
            imageHeight = 800
        )

        val resolved = photo.withImageDimensions(width = 0, height = 1200)

        assertEquals(1200, resolved.imageWidth)
        assertEquals(800, resolved.imageHeight)
    }
}
