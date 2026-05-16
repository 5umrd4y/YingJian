package com.yingjian.feature.photobook.export

import com.yingjian.feature.photobook.model.FitMode

data class PdfRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

object PdfImageRenderMath {
    fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        maxDecodeDim: Int
    ): Int {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) return 1

        var inSampleSize = 1
        while (sourceWidth / inSampleSize > maxDecodeDim || sourceHeight / inSampleSize > maxDecodeDim) {
            inSampleSize *= 2
        }

        while (sourceWidth / (inSampleSize * 2) >= targetWidth &&
            sourceHeight / (inSampleSize * 2) >= targetHeight
        ) {
            inSampleSize *= 2
        }

        return inSampleSize.coerceAtLeast(1)
    }

    fun destinationRect(
        slotLeft: Float,
        slotTop: Float,
        slotWidth: Float,
        slotHeight: Float,
        imageWidth: Int,
        imageHeight: Int,
        fitMode: FitMode,
        cropScale: Float,
        offsetX: Float,
        offsetY: Float
    ): PdfRect {
        if (imageWidth <= 0 || imageHeight <= 0 || slotWidth <= 0f || slotHeight <= 0f) {
            return PdfRect(slotLeft, slotTop, slotLeft + slotWidth, slotTop + slotHeight)
        }

        val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val slotAspect = slotWidth / slotHeight
        val baseScale = when (fitMode) {
            FitMode.Crop -> if (imageAspect > slotAspect) {
                slotHeight / imageHeight
            } else {
                slotWidth / imageWidth
            }
            FitMode.Fit -> if (imageAspect > slotAspect) {
                slotWidth / imageWidth
            } else {
                slotHeight / imageHeight
            }
        }
        val finalScale = baseScale * cropScale.coerceAtLeast(0.01f)
        val renderWidth = imageWidth * finalScale
        val renderHeight = imageHeight * finalScale
        val left = slotLeft + (slotWidth - renderWidth) / 2f + offsetX
        val top = slotTop + (slotHeight - renderHeight) / 2f + offsetY

        return PdfRect(
            left = left,
            top = top,
            right = left + renderWidth,
            bottom = top + renderHeight
        )
    }
}
