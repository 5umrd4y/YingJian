package com.yingjian.feature.photobook

import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PageTemplate

sealed interface TemplateChangeResult {
    data class Changed(val page: PageState) : TemplateChangeResult
    data class Overflow(val page: PageState, val requestedTemplate: PageTemplate, val overflowSlots: List<ImageSlot>) : TemplateChangeResult
}

sealed interface MoveResult {
    data class Moved(val sourcePage: PageState, val targetPage: PageState) : MoveResult
    data object SourceEmpty : MoveResult
    data object TargetFull : MoveResult
}

object PhotobookSlotActions {
    fun emptySlotsFor(template: PageTemplate): List<ImageSlot> =
        template.slotIds.map { ImageSlot(slotId = it, imageRef = null) }

    fun changeTemplate(page: PageState, newTemplate: PageTemplate): TemplateChangeResult {
        val existingById = page.slots.associateBy { it.slotId }
        val activeSlots = newTemplate.slotIds.map { slotId ->
            existingById[slotId] ?: ImageSlot(slotId = slotId, imageRef = null)
        }
        val inactiveSlots = page.slots.filterNot { it.slotId in newTemplate.slotIds }
        return TemplateChangeResult.Changed(page.copy(template = newTemplate, slots = activeSlots + inactiveSlots))
    }

    fun visibleSlots(page: PageState): List<ImageSlot> =
        page.template.slotIds.map { slotId ->
            page.slots.firstOrNull { it.slotId == slotId } ?: ImageSlot(slotId = slotId, imageRef = null)
        }

    fun fillSlot(page: PageState, slotId: String, imageRef: ImageRef): PageState {
        val existing = page.slots.associateBy { it.slotId }
        val updated = existing[slotId]?.copy(imageRef = imageRef, cropScale = 1f, cropOffsetX = 0f, cropOffsetY = 0f)
            ?: ImageSlot(slotId = slotId, imageRef = imageRef)
        val allSlots = page.slots.filterNot { it.slotId == slotId } + updated
        val activeSlots = page.template.slotIds.map { id -> allSlots.first { it.slotId == id } }
        val inactiveSlots = allSlots.filterNot { it.slotId in page.template.slotIds }
        return page.copy(slots = activeSlots + inactiveSlots)
    }

    fun clearSlot(page: PageState, slotId: String): PageState = page.copy(
        slots = page.slots.map { slot ->
            if (slot.slotId == slotId) slot.copy(imageRef = null, cropScale = 1f, cropOffsetX = 0f, cropOffsetY = 0f) else slot
        }
    )

    fun emptyPage(pageNumber: Int, template: PageTemplate): PageState = PageState(
        pageNumber = pageNumber,
        template = template,
        slots = emptySlotsFor(template),
        trimWidthMm = 285f,
        trimHeightMm = 210f
    )

    fun insertPageAfter(pages: List<PageState>, currentPageIndex: Int, newPage: PageState): List<PageState> {
        val insertIndex = (currentPageIndex + 1).coerceIn(0, pages.size)
        return (pages.take(insertIndex) + newPage + pages.drop(insertIndex)).mapIndexed { index, page ->
            page.copy(pageNumber = index + 1)
        }
    }

    fun moveImage(sourcePage: PageState, sourceSlotId: String, targetPage: PageState): MoveResult {
        val sourceSlot = sourcePage.slots.firstOrNull { it.slotId == sourceSlotId } ?: return MoveResult.SourceEmpty
        if (sourceSlot.imageRef == null) return MoveResult.SourceEmpty
        val targetSlot = targetPage.slots.firstOrNull { it.slotId in targetPage.template.slotIds && it.imageRef == null } ?: return MoveResult.TargetFull

        val updatedSource = sourcePage.copy(
            slots = sourcePage.slots.map { slot ->
                if (slot.slotId == sourceSlotId) slot.copy(imageRef = null, cropScale = 1f, cropOffsetX = 0f, cropOffsetY = 0f) else slot
            }
        )
        val updatedTarget = targetPage.copy(
            slots = targetPage.slots.map { slot ->
                if (slot.slotId == targetSlot.slotId) sourceSlot.copy(slotId = targetSlot.slotId) else slot
            }
        )

        return MoveResult.Moved(updatedSource, updatedTarget)
    }

    fun swapSlots(page: PageState, firstSlotId: String, secondSlotId: String): PageState {
        val first = page.slots.firstOrNull { it.slotId == firstSlotId } ?: return page
        val second = page.slots.firstOrNull { it.slotId == secondSlotId } ?: return page
        return page.copy(
            slots = page.slots.map { slot ->
                when (slot.slotId) {
                    firstSlotId -> second.copy(slotId = firstSlotId)
                    secondSlotId -> first.copy(slotId = secondSlotId)
                    else -> slot
                }
            }
        )
    }
}
