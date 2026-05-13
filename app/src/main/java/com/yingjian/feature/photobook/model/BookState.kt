package com.yingjian.feature.photobook.model

enum class LayoutMode { AUTO, MANUAL }

enum class PaperSize(val widthMm: Float, val heightMm: Float) {
    TWELVE_INCH_LANDSCAPE(285f, 210f)  // 方12寸横版
}

data class PageState(
    val pageNumber: Int,
    val elements: List<PageElement>,
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float = 3.0f
)

data class BookState(
    val photobook: com.yingjian.core.data.database.PhotobookEntity,
    val pages: List<PageState>,
    val currentPage: Int,
    val mode: LayoutMode,
    val selectedElementIndex: Int? = null,
    val previousManualState: BookState? = null
)
