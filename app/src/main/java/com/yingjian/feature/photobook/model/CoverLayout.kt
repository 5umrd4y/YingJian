package com.yingjian.feature.photobook.model

import android.graphics.Paint
import android.text.Layout
import androidx.compose.ui.text.style.TextAlign

enum class CoverPageType { Cover, BackCover }

enum class CoverTextRole { Title, Subtitle, Divider, Date }

enum class PhotobookTextAlign {
    Start,
    Center,
    End
}

fun PhotobookTextAlign.toComposeTextAlign(): TextAlign = when (this) {
    PhotobookTextAlign.Start -> TextAlign.Start
    PhotobookTextAlign.Center -> TextAlign.Center
    PhotobookTextAlign.End -> TextAlign.End
}

fun PhotobookTextAlign.toPaintAlign(): Paint.Align = when (this) {
    PhotobookTextAlign.Start -> Paint.Align.LEFT
    PhotobookTextAlign.Center -> Paint.Align.CENTER
    PhotobookTextAlign.End -> Paint.Align.RIGHT
}

fun PhotobookTextAlign.toStaticLayoutAlignment(): Layout.Alignment = when (this) {
    PhotobookTextAlign.Start -> Layout.Alignment.ALIGN_NORMAL
    PhotobookTextAlign.Center -> Layout.Alignment.ALIGN_CENTER
    PhotobookTextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
}

data class CoverLayout(
    val pageType: CoverPageType,
    val backgroundColor: String,
    val textElements: List<CoverTextElement>
)

data class CoverTextElement(
    val id: String,
    val role: CoverTextRole,
    val text: String,
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float,
    val fontSizeMm: Float,
    val letterSpacing: Float,
    val textAlign: PhotobookTextAlign
)

fun CoverLayout.moveText(textId: String, xMm: Float, yMm: Float): CoverLayout = copy(
    textElements = textElements.map { element ->
        if (element.id == textId) element.copy(xMm = xMm, yMm = yMm) else element
    }
)

fun CoverLayout.updateText(textId: String, text: String): CoverLayout = copy(
    textElements = textElements.map { element ->
        if (element.id == textId) element.copy(text = text) else element
    }
)

object CoverLayoutDefaults {
    const val BACKGROUND = "#AAA194"

    fun defaultCover(title: String, subtitle: String): CoverLayout = CoverLayout(
        pageType = CoverPageType.Cover,
        backgroundColor = BACKGROUND,
        textElements = listOf(
            CoverTextElement("cover-title", CoverTextRole.Title, title, 62.5f, 82f, 160f, 18f, 10f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("cover-divider", CoverTextRole.Divider, "", 117.5f, 105f, 50f, 1f, 1f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("cover-subtitle", CoverTextRole.Subtitle, subtitle, 72.5f, 112f, 140f, 12f, 5f, 0f, PhotobookTextAlign.Center)
        )
    )

    fun defaultBackCover(title: String, subtitle: String, dateText: String): CoverLayout = CoverLayout(
        pageType = CoverPageType.BackCover,
        backgroundColor = BACKGROUND,
        textElements = listOf(
            CoverTextElement("back-title", CoverTextRole.Title, title, 72.5f, 84f, 140f, 14f, 8f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("back-subtitle", CoverTextRole.Subtitle, subtitle, 72.5f, 103f, 140f, 10f, 5f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("back-date", CoverTextRole.Date, dateText, 92.5f, 118f, 100f, 8f, 4f, 0f, PhotobookTextAlign.Center)
        )
    )
}
