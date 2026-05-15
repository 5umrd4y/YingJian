package com.yingjian.feature.photobook

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
        val filled = page.slots.filter { it.imageRef != null }
        if (filled.size > newTemplate.capacity) {
            return TemplateChangeResult.Overflow(
                page = page,
                requestedTemplate = newTemplate,
                overflowSlots = filled.drop(newTemplate.capacity)
            )
        }

        val newSlots = newTemplate.slotIds.mapIndexed { index, slotId ->
            val old = filled.getOrNull(index)
            if (old == null) ImageSlot(slotId = slotId, imageRef = null) else old.copy(slotId = slotId)
        }

        return TemplateChangeResult.Changed(page.copy(template = newTemplate, slots = newSlots))
    }

    fun moveImage(sourcePage: PageState, sourceSlotId: String, targetPage: PageState): MoveResult {
        val sourceSlot = sourcePage.slots.firstOrNull { it.slotId == sourceSlotId } ?: return MoveResult.SourceEmpty
        if (sourceSlot.imageRef == null) return MoveResult.SourceEmpty
        val targetSlot = targetPage.slots.firstOrNull { it.imageRef == null } ?: return MoveResult.TargetFull

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
