package com.yingjian.feature.photobook

import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PageTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotobookSlotActionsTest {
    @Test
    fun `single to grid keeps image in first slot`() {
        val page = page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 1)))

        val result = PhotobookSlotActions.changeTemplate(page, PageTemplate.GridFour)

        val updated = result as TemplateChangeResult.Changed
        assertEquals(PageTemplate.GridFour, updated.page.template)
        assertEquals(4, updated.page.slots.size)
        assertEquals("content://image/1", updated.page.slots[0].imageRef?.imageUri)
        assertNull(updated.page.slots[1].imageRef)
    }

    @Test
    fun `move fills first empty target slot and clears source`() {
        val source = page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 1)))
        val target = page(PageTemplate.GridFour, listOf(slot("slot-1", 2), empty("slot-2"), empty("slot-3"), empty("slot-4")), pageNumber = 2)

        val result = PhotobookSlotActions.moveImage(source, "slot-1", target)

        val moved = result as MoveResult.Moved
        assertNull(moved.sourcePage.slots.first().imageRef)
        assertEquals("content://image/1", moved.targetPage.slots[1].imageRef?.imageUri)
    }

    @Test
    fun `move to full page returns target full`() {
        val source = page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 1)))
        val target = page(PageTemplate.TwoVertical, listOf(slot("slot-1", 2), slot("slot-2", 3)), pageNumber = 2)

        val result = PhotobookSlotActions.moveImage(source, "slot-1", target)

        assertEquals(MoveResult.TargetFull, result)
    }

    @Test
    fun `switching grid to single keeps inactive slot images`() {
        val page = page(
            PageTemplate.GridFour,
            listOf(slot("slot-1", 1), slot("slot-2", 2), slot("slot-3", 3), slot("slot-4", 4))
        )

        val result = PhotobookSlotActions.changeTemplate(page, PageTemplate.SingleLandscape)

        val changed = result as TemplateChangeResult.Changed
        assertEquals(PageTemplate.SingleLandscape, changed.page.template)
        assertEquals(4, changed.page.slots.size)
        assertEquals("content://image/1", changed.page.slots.first { it.slotId == "slot-1" }.imageRef?.imageUri)
        assertEquals("content://image/2", changed.page.slots.first { it.slotId == "slot-2" }.imageRef?.imageUri)
    }

    @Test
    fun `visible slots only returns active template slots`() {
        val page = page(
            PageTemplate.SingleLandscape,
            listOf(slot("slot-1", 1), slot("slot-2", 2))
        )

        val visible = PhotobookSlotActions.visibleSlots(page)

        assertEquals(listOf("slot-1"), visible.map { it.slotId })
    }

    @Test
    fun `fill slot updates exact target slot`() {
        val page = page(PageTemplate.GridFour, listOf(empty("slot-1"), empty("slot-2"), empty("slot-3"), empty("slot-4")))

        val updated = PhotobookSlotActions.fillSlot(page, "slot-3", ImageRef(9, "content://image/9", 0))

        assertEquals("content://image/9", updated.slots.first { it.slotId == "slot-3" }.imageRef?.imageUri)
        assertNull(updated.slots.first { it.slotId == "slot-1" }.imageRef)
    }

    @Test
    fun `fill slot in pages updates requested page instead of first page`() {
        val pages = listOf(
            page(PageTemplate.GridFour, listOf(empty("slot-1"), empty("slot-2"), empty("slot-3"), empty("slot-4")), pageNumber = 1),
            page(PageTemplate.GridFour, listOf(empty("slot-1"), empty("slot-2"), empty("slot-3"), empty("slot-4")), pageNumber = 2)
        )

        val updated = PhotobookSlotActions.fillSlotInPage(
            pages = pages,
            pageIndex = 1,
            slotId = "slot-2",
            imageRef = ImageRef(9, "content://image/9", 0)
        )

        assertNull(updated[0].slots.first { it.slotId == "slot-2" }.imageRef)
        assertEquals("content://image/9", updated[1].slots.first { it.slotId == "slot-2" }.imageRef?.imageUri)
    }

    @Test
    fun `fill single slot switches to portrait template for portrait image`() {
        val page = page(PageTemplate.SingleLandscape, listOf(empty("slot-1")))

        val updated = PhotobookSlotActions.fillSlot(
            page,
            "slot-1",
            ImageRef(
                memoryId = 9,
                imageUri = "content://image/portrait",
                sourceImageIndex = 0,
                imageWidth = 800,
                imageHeight = 1200
            )
        )

        assertEquals(PageTemplate.SinglePortrait, updated.template)
        assertEquals("content://image/portrait", updated.slots.single().imageRef?.imageUri)
    }

    @Test
    fun `fill single slot switches to landscape template for landscape image`() {
        val page = page(PageTemplate.SinglePortrait, listOf(empty("slot-1")))

        val updated = PhotobookSlotActions.fillSlot(
            page,
            "slot-1",
            ImageRef(
                memoryId = 9,
                imageUri = "content://image/landscape",
                sourceImageIndex = 0,
                imageWidth = 1200,
                imageHeight = 800
            )
        )

        assertEquals(PageTemplate.SingleLandscape, updated.template)
        assertEquals("content://image/landscape", updated.slots.single().imageRef?.imageUri)
    }

    @Test
    fun `insert page after current renumbers pages`() {
        val pages = listOf(
            page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 1)), pageNumber = 1),
            page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 2)), pageNumber = 2)
        )
        val newPage = PhotobookSlotActions.emptyPage(pageNumber = 0, template = PageTemplate.SingleLandscape)

        val updated = PhotobookSlotActions.insertPageAfter(pages, currentPageIndex = 0, newPage = newPage)

        assertEquals(listOf(1, 2, 3), updated.map { it.pageNumber })
        assertTrue(updated[1].slots.single().isEmpty)
    }

    @Test
    fun `delete page at current index removes page and renumbers remaining pages`() {
        val pages = listOf(
            page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 1)), pageNumber = 1),
            page(PageTemplate.SingleLandscape, listOf(empty("slot-1")), pageNumber = 2),
            page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 3)), pageNumber = 3)
        )

        val updated = PhotobookSlotActions.deletePageAt(pages, pageIndex = 1)

        assertEquals(2, updated.size)
        assertEquals(listOf(1, 2), updated.map { it.pageNumber })
        assertEquals("content://image/3", updated[1].slots.single().imageRef?.imageUri)
    }

    @Test
    fun `delete last remaining page keeps one empty page`() {
        val pages = listOf(page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 1)), pageNumber = 1))

        val updated = PhotobookSlotActions.deletePageAt(pages, pageIndex = 0)

        assertEquals(1, updated.size)
        assertEquals(1, updated.single().pageNumber)
        assertTrue(updated.single().slots.single().isEmpty)
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
