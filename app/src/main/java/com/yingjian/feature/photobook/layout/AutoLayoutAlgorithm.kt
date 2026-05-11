package com.yingjian.feature.photobook.layout

import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize

/**
 * Auto layout algorithm for arranging photos in a photobook.
 * TODO (Task 8): Implement intelligent layout algorithm.
 * Currently provides a simple stub that places one photo per page.
 */
object AutoLayoutAlgorithm {

    fun layout(
        memories: List<MemoryRecordEntity>,
        paperSize: PaperSize,
        photobook: PhotobookEntity
    ): BookState {
        val pages = memories.mapIndexed { index, memory ->
            PageState(
                pageNumber = index + 1,
                elements = listOf(
                    ImageElement(
                        memoryId = memory.id,
                        imageUri = memory.imageUri,
                        xMm = 10f,
                        yMm = 10f,
                        widthMm = paperSize.widthMm - 20f,
                        heightMm = paperSize.heightMm - 20f,
                        rotationDeg = 0f,
                        zIndex = 0
                    )
                ),
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
}
