package com.yingjian.feature.photobook.layout

import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults

data class LayoutInput(
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float,
    val safeMarginMm: Float,
    val bottomTextReserveMm: Float = PhotobookLayoutDefaults.BOTTOM_TEXT_RESERVE_MM,
    val gutterMm: Float = PhotobookLayoutDefaults.GUTTER_MM,
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

    fun calculateSlots(input: LayoutInput): List<SlotRectMm> {
        val contentX = input.safeMarginMm
        val contentY = input.safeMarginMm
        val contentWidth = input.trimWidthMm - input.safeMarginMm * 2f
        val contentHeight = input.trimHeightMm - input.safeMarginMm * 2f - input.bottomTextReserveMm

        fun centered(widthMm: Float, heightMm: Float, slotId: String = "slot-1") = SlotRectMm(
            slotId = slotId,
            xMm = contentX + (contentWidth - widthMm) / 2f,
            yMm = contentY + (contentHeight - heightMm) / 2f,
            widthMm = widthMm,
            heightMm = heightMm
        )

        return when (input.template) {
            PageTemplate.SingleLandscape -> {
                val width = kotlin.math.min(contentWidth, contentHeight * 1.5f)
                val height = width / 1.5f
                listOf(centered(width, height))
            }
            PageTemplate.SinglePortrait -> {
                val height = kotlin.math.min(contentHeight, contentWidth * 1.5f)
                val width = height * (2f / 3f)
                listOf(centered(width, height))
            }
            PageTemplate.TwoHorizontal -> {
                val slotHeight = (contentHeight - input.gutterMm) / 2f
                listOf(
                    SlotRectMm("slot-1", contentX, contentY, contentWidth, slotHeight),
                    SlotRectMm("slot-2", contentX, contentY + slotHeight + input.gutterMm, contentWidth, slotHeight)
                )
            }
            PageTemplate.TwoVertical -> {
                val slotWidth = (contentWidth - input.gutterMm) / 2f
                listOf(
                    SlotRectMm("slot-1", contentX, contentY, slotWidth, contentHeight),
                    SlotRectMm("slot-2", contentX + slotWidth + input.gutterMm, contentY, slotWidth, contentHeight)
                )
            }
            PageTemplate.GridFour -> {
                val slotWidth = (contentWidth - input.gutterMm) / 2f
                val slotHeight = (contentHeight - input.gutterMm) / 2f
                listOf(
                    SlotRectMm("slot-1", contentX, contentY, slotWidth, slotHeight),
                    SlotRectMm("slot-2", contentX + slotWidth + input.gutterMm, contentY, slotWidth, slotHeight),
                    SlotRectMm("slot-3", contentX, contentY + slotHeight + input.gutterMm, slotWidth, slotHeight),
                    SlotRectMm("slot-4", contentX + slotWidth + input.gutterMm, contentY + slotHeight + input.gutterMm, slotWidth, slotHeight)
                )
            }
        }
    }
}
