package com.yingjian.feature.photobook.model

data class PageLayoutDocument(
    val version: Int = 1,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement> = emptyList()
)
