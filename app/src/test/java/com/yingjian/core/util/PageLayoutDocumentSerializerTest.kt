package com.yingjian.core.util

import com.yingjian.feature.photobook.model.FitMode
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageLayoutDocument
import com.yingjian.feature.photobook.model.PageTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class PageLayoutDocumentSerializerTest {
    @Test
    fun `serializes and deserializes slot crop state`() {
        val document = PageLayoutDocument(
            template = PageTemplate.GridFour,
            slots = listOf(
                ImageSlot(
                    slotId = "slot-1",
                    imageRef = ImageRef(
                        memoryId = 7L,
                        imageUri = "content://memory/7/1",
                        sourceImageIndex = 1
                    ),
                    cropScale = 1.35f,
                    cropOffsetX = -2.5f,
                    cropOffsetY = 4.25f,
                    fitMode = FitMode.Crop
                )
            )
        )

        val json = PageLayoutDocumentSerializer.serialize(document)
        val decoded = PageLayoutDocumentSerializer.deserialize(json)

        assertEquals(PageTemplate.GridFour, decoded.template)
        assertEquals("slot-1", decoded.slots.first().slotId)
        assertEquals("content://memory/7/1", decoded.slots.first().imageRef?.imageUri)
        assertEquals(1.35f, decoded.slots.first().cropScale, 0.001f)
        assertEquals(-2.5f, decoded.slots.first().cropOffsetX, 0.001f)
        assertEquals(4.25f, decoded.slots.first().cropOffsetY, 0.001f)
    }

    @Test
    fun `page layout document defaults to version 2`() {
        val document = PageLayoutDocument(
            template = PageTemplate.SingleLandscape,
            slots = listOf(ImageSlot(slotId = "slot-1", imageRef = null))
        )

        assertEquals(2, document.version)
    }
}
