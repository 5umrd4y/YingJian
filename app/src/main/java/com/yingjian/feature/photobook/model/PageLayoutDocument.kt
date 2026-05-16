package com.yingjian.feature.photobook.model

data class PageLayoutDocument(
    val version: Int = 2,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement> = emptyList()
) {
    val elements: List<PageElement>
        get() = textElements
}
