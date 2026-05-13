package com.yingjian.feature.photobook.layout

import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PageElement
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize
import com.yingjian.feature.photobook.model.TextElement

/**
 * MVP Auto Layout: one image per page, centered fit, with mood text below.
 *
 * Algorithm:
 * - For each MemoryRecord, create one page
 * - Image: Fit Center within trim area, respecting 3mm bleed margin
 * - Text: Fixed 8mm below image, 70% of trim width, centered
 * - Bleed safety: image stays within trim - bleed area
 */
object AutoLayoutAlgorithm {

    private const val BLEED_MM = 3f
    private const val TEXT_GAP_MM = 8f
    private const val TEXT_BOTTOM_MARGIN_MM = 5f
    private const val TEXT_WIDTH_RATIO = 0.7f

    fun layout(
        memories: List<MemoryRecordEntity>,
        paperSize: PaperSize,
        photobook: PhotobookEntity
    ): BookState {
        val safeWidth = paperSize.widthMm - BLEED_MM * 2
        val safeHeight = paperSize.heightMm - BLEED_MM * 2

        val pages = memories.mapIndexed { index, memory ->
            val aspectRatio = if (memory.imageHeight > 0) {
                memory.imageWidth.toFloat() / memory.imageHeight.toFloat()
            } else {
                1f
            }

            // Fit Center calculation
            val imageWidthMm = minOf(safeWidth, safeHeight * aspectRatio)
            val imageHeightMm = imageWidthMm / aspectRatio

            // Check if height exceeds safe area and adjust
            val (finalWidth, finalHeight) = if (imageHeightMm > safeHeight) {
                val h = safeHeight
                val w = h * aspectRatio
                w to h
            } else {
                imageWidthMm to imageHeightMm
            }

            // Center the image in the safe area
            val imageXMm = (paperSize.widthMm - finalWidth) / 2
            val imageYMm = BLEED_MM + (safeHeight - finalHeight) / 2

            val elements = mutableListOf<PageElement>()

            elements.add(
                ImageElement(
                    memoryId = memory.id,
                    imageUri = memory.imageUri,
                    xMm = imageXMm,
                    yMm = imageYMm,
                    widthMm = finalWidth,
                    heightMm = finalHeight,
                    rotationDeg = 0f,
                    zIndex = 0
                )
            )

            // Add mood text below image
            if (!memory.moodText.isNullOrBlank()) {
                val textYMm = imageYMm + finalHeight + TEXT_GAP_MM
                // Ensure text bottom is at least TEXT_BOTTOM_MARGIN_MM from page edge
                val textBottom = textYMm + 6f // approximate text height in mm
                if (textBottom <= paperSize.heightMm - TEXT_BOTTOM_MARGIN_MM) {
                    elements.add(
                        TextElement(
                            text = memory.moodText,
                            xMm = (paperSize.widthMm - paperSize.widthMm * TEXT_WIDTH_RATIO) / 2,
                            yMm = textYMm,
                            widthMm = paperSize.widthMm * TEXT_WIDTH_RATIO,
                            heightMm = 10f,
                            rotationDeg = 0f,
                            zIndex = 1
                        )
                    )
                }
            }

            PageState(
                pageNumber = index + 1,
                elements = elements,
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
        memory: MemoryRecordEntity,
        paperSize: PaperSize,
        pageNumber: Int
    ): PageState {
        val safeWidth = paperSize.widthMm - BLEED_MM * 2
        val safeHeight = paperSize.heightMm - BLEED_MM * 2

        val aspectRatio = if (memory.imageHeight > 0) {
            memory.imageWidth.toFloat() / memory.imageHeight.toFloat()
        } else {
            1f
        }

        var imageWidthMm = minOf(safeWidth, safeHeight * aspectRatio)
        var imageHeightMm = imageWidthMm / aspectRatio

        if (imageHeightMm > safeHeight) {
            imageHeightMm = safeHeight
            imageWidthMm = imageHeightMm * aspectRatio
        }

        val imageXMm = (paperSize.widthMm - imageWidthMm) / 2
        val imageYMm = BLEED_MM + (safeHeight - imageHeightMm) / 2

        val elements = mutableListOf<PageElement>()

        elements.add(
            ImageElement(
                memoryId = memory.id,
                imageUri = memory.imageUri,
                xMm = imageXMm,
                yMm = imageYMm,
                widthMm = imageWidthMm,
                heightMm = imageHeightMm,
                rotationDeg = 0f,
                zIndex = 0
            )
        )

        if (!memory.moodText.isNullOrBlank()) {
            val textYMm = imageYMm + imageHeightMm + TEXT_GAP_MM
            val textBottom = textYMm + 6f
            if (textBottom <= paperSize.heightMm - TEXT_BOTTOM_MARGIN_MM) {
                elements.add(
                    TextElement(
                        text = memory.moodText,
                        xMm = (paperSize.widthMm - paperSize.widthMm * TEXT_WIDTH_RATIO) / 2,
                        yMm = textYMm,
                        widthMm = paperSize.widthMm * TEXT_WIDTH_RATIO,
                        heightMm = 10f,
                        rotationDeg = 0f,
                        zIndex = 1
                    )
                )
            }
        }

        return PageState(
            pageNumber = pageNumber,
            elements = elements,
            trimWidthMm = paperSize.widthMm,
            trimHeightMm = paperSize.heightMm
        )
    }
}
