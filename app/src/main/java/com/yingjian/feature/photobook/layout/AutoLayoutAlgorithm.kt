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
 * Auto Layout: one image per page, template chosen by image aspect ratio.
 */
object AutoLayoutAlgorithm {

    fun layout(
        memories: List<MemoryRecordEntity>,
        paperSize: PaperSize,
        photobook: PhotobookEntity
    ): BookState {
        val pages = memories.mapIndexed { index, memory ->
            val imageRef = ImageRef(
                memoryId = memory.id,
                imageUri = memory.imageUri,
                sourceImageIndex = 0,
                imageWidth = memory.imageWidth,
                imageHeight = memory.imageHeight
            )
            PageState(
                pageNumber = index + 1,
                template = chooseSingleTemplate(memory.imageWidth, memory.imageHeight),
                slots = listOf(
                    ImageSlot(
                        slotId = "slot-1",
                        imageRef = imageRef
                    )
                ),
                textElements = buildMoodTextElements(memory, paperSize),
                trimWidthMm = paperSize.widthMm,
                trimHeightMm = paperSize.heightMm
            )
        }

        return BookState(
            photobook = photobook,
            pages = pages,
            currentPage = 0,
            mode = LayoutMode.AUTO
        )
    }

    fun createSinglePhotoPage(
        imageRef: ImageRef,
        moodText: String?,
        paperSize: PaperSize,
        pageNumber: Int,
        imageWidth: Int? = imageRef.imageWidth,
        imageHeight: Int? = imageRef.imageHeight
    ): PageState = PageState(
        pageNumber = pageNumber,
        template = chooseSingleTemplate(imageWidth, imageHeight),
        slots = listOf(ImageSlot(slotId = "slot-1", imageRef = imageRef)),
        textElements = if (moodText.isNullOrBlank()) {
            emptyList()
        } else {
            listOf(
                TextElement(
                    text = moodText,
                    xMm = paperSize.widthMm * 0.15f,
                    yMm = paperSize.heightMm - 22f,
                    widthMm = paperSize.widthMm * 0.7f,
                    heightMm = 10f,
                    rotationDeg = 0f,
                    zIndex = 1
                )
            )
        },
        trimWidthMm = paperSize.widthMm,
        trimHeightMm = paperSize.heightMm
    )

    private fun chooseSingleTemplate(imageWidth: Int?, imageHeight: Int?): PageTemplate {
        val width = imageWidth ?: return PageTemplate.SingleLandscape
        val height = imageHeight ?: return PageTemplate.SingleLandscape
        if (width <= 0 || height <= 0) return PageTemplate.SingleLandscape
        return if (width.toFloat() / height.toFloat() >= 1f) {
            PageTemplate.SingleLandscape
        } else {
            PageTemplate.SinglePortrait
        }
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
