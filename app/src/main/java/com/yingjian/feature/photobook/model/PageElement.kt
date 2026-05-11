package com.yingjian.feature.photobook.model

import androidx.compose.ui.text.style.TextAlign

sealed class PageElement {
    abstract val xMm: Float
    abstract val yMm: Float
    abstract val widthMm: Float
    abstract val heightMm: Float
    abstract val rotationDeg: Float
    abstract val zIndex: Int
}

data class ImageElement(
    val memoryId: Long,
    val imageUri: String,
    override val xMm: Float,
    override val yMm: Float,
    override val widthMm: Float,
    override val heightMm: Float,
    override val rotationDeg: Float,
    override val zIndex: Int,
    val contentScale: Float = 1.0f,
    val contentOffsetX: Float = 0f,
    val contentOffsetY: Float = 0f
) : PageElement()

data class TextElement(
    val text: String,
    val fontSizeMm: Float = 4.0f,
    val textAlign: TextAlign = TextAlign.Center,
    override val xMm: Float,
    override val yMm: Float,
    override val widthMm: Float,
    override val heightMm: Float,
    override val rotationDeg: Float = 0f,
    override val zIndex: Int
) : PageElement()
