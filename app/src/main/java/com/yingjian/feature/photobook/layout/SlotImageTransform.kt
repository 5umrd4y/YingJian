package com.yingjian.feature.photobook.layout

object SlotImageTransform {
    const val MIN_CROP_SCALE = 1f
    const val MAX_CROP_SCALE = 3f

    fun clampScale(value: Float): Float = value.coerceIn(MIN_CROP_SCALE, MAX_CROP_SCALE)

    fun clampOffset(valueMm: Float, slotSizeMm: Float, cropScale: Float): Float {
        val extra = (slotSizeMm * cropScale - slotSizeMm) / 2f
        return valueMm.coerceIn(-extra, extra)
    }
}
