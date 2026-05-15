package com.yingjian.feature.photobook.layout

import com.yingjian.feature.photobook.model.PageTemplate

data class LayoutInput(
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float,
    val safeMarginMm: Float,
    val template: PageTemplate
)

data class SlotRectMm(
    val slotId: String,
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float
)

object TemplateLayoutEngine {
    private const val GUTTER_MM = 8f
    private const val BOTTOM_TEXT_RESERVE_MM = 20f

    fun calculateSlots(input: LayoutInput): List<SlotRectMm> {
        val left = maxOf(input.bleedMm, input.safeMarginMm)
        val top = maxOf(input.bleedMm, input.safeMarginMm)
        val right = input.trimWidthMm - maxOf(input.bleedMm, input.safeMarginMm)
        val bottom = input.trimHeightMm - maxOf(input.bleedMm, input.safeMarginMm) - BOTTOM_TEXT_RESERVE_MM
        val width = right - left
        val height = bottom - top

        return when (input.template) {
            PageTemplate.Single -> listOf(
                SlotRectMm("slot-1", left, top, width, height)
            )
            PageTemplate.TwoHorizontal -> {
                val slotHeight = (height - GUTTER_MM) / 2f
                listOf(
                    SlotRectMm("slot-1", left, top, width, slotHeight),
                    SlotRectMm("slot-2", left, top + slotHeight + GUTTER_MM, width, slotHeight)
                )
            }
            PageTemplate.TwoVertical -> {
                val slotWidth = (width - GUTTER_MM) / 2f
                listOf(
                    SlotRectMm("slot-1", left, top, slotWidth, height),
                    SlotRectMm("slot-2", left + slotWidth + GUTTER_MM, top, slotWidth, height)
                )
            }
            PageTemplate.GridFour -> {
                val slotWidth = (width - GUTTER_MM) / 2f
                val slotHeight = (height - GUTTER_MM) / 2f
                listOf(
                    SlotRectMm("slot-1", left, top, slotWidth, slotHeight),
                    SlotRectMm("slot-2", left + slotWidth + GUTTER_MM, top, slotWidth, slotHeight),
                    SlotRectMm("slot-3", left, top + slotHeight + GUTTER_MM, slotWidth, slotHeight),
                    SlotRectMm("slot-4", left + slotWidth + GUTTER_MM, top + slotHeight + GUTTER_MM, slotWidth, slotHeight)
                )
            }
        }
    }
}
