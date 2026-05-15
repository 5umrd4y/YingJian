package com.yingjian.feature.photobook

import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PageTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotobookSlotActionsTest {
    @Test
    fun `single to grid keeps image in first slot`() {
        val page = page(PageTemplate.Single, listOf(slot("slot-1", 1)))

        val result = PhotobookSlotActions.changeTemplate(page, PageTemplate.GridFour)

        val updated = result as TemplateChangeResult.Changed
        assertEquals(PageTemplate.GridFour, updated.page.template)
        assertEquals(4, updated.page.slots.size)
        assertEquals("content://image/1", updated.page.slots[0].imageRef?.imageUri)
        assertNull(updated.page.slots[1].imageRef)
    }

    @Test
    fun `move fills first empty target slot and clears source`() {
        val source = page(PageTemplate.Single, listOf(slot("slot-1", 1)))
        val target = page(PageTemplate.GridFour, listOf(slot("slot-1", 2), empty("slot-2"), empty("slot-3"), empty("slot-4")), pageNumber = 2)

        val result = PhotobookSlotActions.moveImage(source, "slot-1", target)

        val moved = result as MoveResult.Moved
        assertNull(moved.sourcePage.slots.first().imageRef)
        assertEquals("content://image/1", moved.targetPage.slots[1].imageRef?.imageUri)
    }

    @Test
    fun `move to full page returns target full`() {
        val source = page(PageTemplate.Single, listOf(slot("slot-1", 1)))
        val target = page(PageTemplate.TwoVertical, listOf(slot("slot-1", 2), slot("slot-2", 3)), pageNumber = 2)

        val result = PhotobookSlotActions.moveImage(source, "slot-1", target)

        assertEquals(MoveResult.TargetFull, result)
    }

    private fun page(template: PageTemplate, slots: List<ImageSlot>, pageNumber: Int = 1) = PageState(
        pageNumber = pageNumber,
        template = template,
        slots = slots,
        trimWidthMm = 285f,
        trimHeightMm = 210f
    )

    private fun slot(id: String, memoryId: Long) = ImageSlot(
        slotId = id,
        imageRef = ImageRef(memoryId, "content://image/$memoryId", 0)
    )

    private fun empty(id: String) = ImageSlot(slotId = id, imageRef = null)
}
