package com.yingjian.feature.photobook.layout

import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize
import com.yingjian.feature.photobook.model.TextElement

/**
 * MVP Auto Layout: one image per page, centered fit, with mood text below.
 *
 * Algorithm:
 * - For each MemoryRecord, create one page
 * - Image: Fit Center within trim area, respecting 3mm bleed margin
 * - Text: Fixed position near bottom of page (heightMm - 22mm)
 * - Bleed safety: image stays within trim - bleed area
 */
object AutoLayoutAlgorithm {

    private const val BLEED_MM = 3f

    fun layout(
        memories: List<MemoryRecordEntity>,
        paperSize: PaperSize,
        photobook: PhotobookEntity
    ): BookState {
        val pages = memories.mapIndexed { index, memory ->
            val page = PageState(
                pageNumber = index + 1,
                template = PageTemplate.Single,
                slots = listOf(
                    ImageSlot(
                        slotId = "slot-1",
                        imageRef = ImageRef(
                            memoryId = memory.id,
                            imageUri = memory.imageUri,
                            sourceImageIndex = 0
                        )
                    )
                ),
                textElements = buildMoodTextElements(memory, paperSize),
                trimWidthMm = paperSize.widthMm,
                trimHeightMm = paperSize.heightMm
            )
            page
        }

        return BookState(
            photobook = photobook,
            pages = pages,
            currentPage = 0,
            mode = LayoutMode.AUTO
        )
    }

    fun createSinglePhotoPage(
        memory: MemoryRecordEntity,
        paperSize: PaperSize,
        pageNumber: Int
    ): PageState {
        return PageState(
            pageNumber = pageNumber,
            template = PageTemplate.Single,
            slots = listOf(
                ImageSlot(
                    slotId = "slot-1",
                    imageRef = ImageRef(
                        memoryId = memory.id,
                        imageUri = memory.imageUri,
                        sourceImageIndex = 0
                    )
                )
            ),
            textElements = buildMoodTextElements(memory, paperSize),
            trimWidthMm = paperSize.widthMm,
            trimHeightMm = paperSize.heightMm
        )
    }

    private fun buildMoodTextElements(memory: MemoryRecordEntity, paperSize: PaperSize): List<TextElement> {
        if (memory.moodText.isNullOrBlank()) return emptyList()
        return listOf(
            TextElement(
                text = memory.moodText,
                xMm = paperSize.widthMm * 0.15f,
                yMm = paperSize.heightMm - 22f,
                widthMm = paperSize.widthMm * 0.7f,
                heightMm = 10f,
                rotationDeg = 0f,
                zIndex = 1
            )
        )
    }
}
