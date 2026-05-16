package com.yingjian.feature.photobook

import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults

data class PreviewSinglePageSize(
    val pageWidthDp: Float,
    val pageHeightDp: Float
)

data class PreviewSpreadSize(
    val spreadWidthDp: Float,
    val spreadHeightDp: Float,
    val pageWidthDp: Float,
    val pageHeightDp: Float
)

object PhotobookPreviewLayout {
    private const val PAGE_ASPECT =
        PhotobookLayoutDefaults.PAGE_WIDTH_MM / PhotobookLayoutDefaults.PAGE_HEIGHT_MM
    private const val SPREAD_ASPECT = PAGE_ASPECT * 2f

    fun fitSinglePage(maxWidthDp: Float, maxHeightDp: Float): PreviewSinglePageSize {
        val fitted = fitAspect(maxWidthDp, maxHeightDp, PAGE_ASPECT)
        return PreviewSinglePageSize(
            pageWidthDp = fitted.first,
            pageHeightDp = fitted.second
        )
    }

    fun fitSpread(maxWidthDp: Float, maxHeightDp: Float): PreviewSpreadSize {
        val fitted = fitAspect(maxWidthDp, maxHeightDp, SPREAD_ASPECT)
        return PreviewSpreadSize(
            spreadWidthDp = fitted.first,
            spreadHeightDp = fitted.second,
            pageWidthDp = fitted.first / 2f,
            pageHeightDp = fitted.second
        )
    }

    private fun fitAspect(maxWidthDp: Float, maxHeightDp: Float, aspect: Float): Pair<Float, Float> {
        if (maxWidthDp <= 0f || maxHeightDp <= 0f) return 0f to 0f
        val heightFromWidth = maxWidthDp / aspect
        return if (heightFromWidth <= maxHeightDp) {
            maxWidthDp to heightFromWidth
        } else {
            maxHeightDp * aspect to maxHeightDp
        }
    }
}
