package com.yingjian.feature.photobook.model

enum class LayoutMode { AUTO, MANUAL }

enum class PaperSize(val widthMm: Float, val heightMm: Float) {
    TWELVE_INCH_LANDSCAPE(285f, 210f)
}

data class PageState(
    val pageNumber: Int,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement> = emptyList(),
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float = 3.0f
) {
    fun toDocument(): PageLayoutDocument = PageLayoutDocument(
        template = template,
        slots = slots,
        textElements = textElements
    )

    /** Compatibility shim: converts slots to ImageElement list for legacy rendering code. */
    val elements: List<PageElement>
        get() {
            val result = mutableListOf<PageElement>()
            val slotWidthMm = trimWidthMm / template.capacity.toFloat()
            slots.forEachIndexed { index, slot ->
                val ref = slot.imageRef
                if (ref != null) {
                    result.add(
                        ImageElement(
                            memoryId = ref.memoryId,
                            imageUri = ref.imageUri,
                            xMm = slot.xMm.takeIf { it != 0f } ?: (slotWidthMm * index),
                            yMm = slot.yMm,
                            widthMm = slot.widthMm ?: slotWidthMm,
                            heightMm = slot.heightMm ?: trimHeightMm,
                            rotationDeg = 0f,
                            zIndex = 0,
                            contentScale = slot.cropScale,
                            contentOffsetX = slot.cropOffsetX,
                            contentOffsetY = slot.cropOffsetY
                        )
                    )
                }
            }
            result.addAll(textElements)
            return result
        }
}

data class EditorSelection(
    val leafIndex: Int,
    val pageIndex: Int?,
    val slotId: String?
)

data class BookState(
    val photobook: com.yingjian.core.data.database.PhotobookEntity,
    val pages: List<PageState>,
    val currentPage: Int,
    val mode: LayoutMode,
    val selectedSlotId: String? = null,
    val previousManualState: BookState? = null
)
