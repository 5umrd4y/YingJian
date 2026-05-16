package com.yingjian.feature.photobook.model

data class ImageRef(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
