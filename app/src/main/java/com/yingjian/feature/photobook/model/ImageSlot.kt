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
    val fitMode: FitMode = FitMode.Crop
) {
    val isEmpty: Boolean get() = imageRef == null
}
