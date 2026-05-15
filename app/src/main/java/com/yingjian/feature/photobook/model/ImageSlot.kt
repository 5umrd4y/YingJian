package com.yingjian.feature.photobook.model

enum class FitMode {
    Crop,
    Fit
}

data class ImageSlot(
    val slotId: String,
    val imageRef: ImageRef?,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val fitMode: FitMode = FitMode.Crop,
    val widthMm: Float? = null,
    val heightMm: Float? = null,
    val xMm: Float = 0f,
    val yMm: Float = 0f
) {
    val isEmpty: Boolean get() = imageRef == null
}
