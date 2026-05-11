package com.yingjian.feature.photobook.model

enum class LayoutMode { AUTO, MANUAL }

enum class PaperSize(val widthMm: Float, val heightMm: Float) {
    A4(297f, 210f),
    A4_PORTRAIT(210f, 297f),
    SIX_INCH_LANDSCAPE(152f, 102f),
    SIX_INCH_PORTRAIT(102f, 152f),
    SQUARE(200f, 200f)
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
