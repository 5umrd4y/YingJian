package com.yingjian.core.util

import androidx.compose.ui.text.style.TextAlign
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.PageElement
import com.yingjian.feature.photobook.model.TextElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class PageElementDto {
    abstract val xMm: Float
    abstract val yMm: Float
    abstract val widthMm: Float
    abstract val heightMm: Float
    abstract val rotationDeg: Float
    abstract val zIndex: Int
}

@Serializable
data class ImageElementDto(
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
) : PageElementDto()

@Serializable
data class TextElementDto(
    val text: String,
    val fontSizeMm: Float = 4.0f,
    val textAlign: String = "Center",
    override val xMm: Float,
    override val yMm: Float,
    override val widthMm: Float,
    override val heightMm: Float,
    override val rotationDeg: Float = 0f,
    override val zIndex: Int
) : PageElementDto()

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object ElementSerializer {
    fun serialize(elements: List<PageElement>): String {
        val dtos = elements.map { toDto(it) }
        return json.encodeToString(dtos)
    }

    fun deserialize(jsonStr: String): List<PageElement> {
        val dtos = json.decodeFromString<List<PageElementDto>>(jsonStr)
        return dtos.map { fromDto(it) }
    }

    private fun toDto(element: PageElement): PageElementDto = when (element) {
        is ImageElement -> ImageElementDto(
            memoryId = element.memoryId,
            imageUri = element.imageUri,
            xMm = element.xMm,
            yMm = element.yMm,
            widthMm = element.widthMm,
            heightMm = element.heightMm,
            rotationDeg = element.rotationDeg,
            zIndex = element.zIndex,
            contentScale = element.contentScale,
            contentOffsetX = element.contentOffsetX,
            contentOffsetY = element.contentOffsetY
        )
        is TextElement -> TextElementDto(
            text = element.text,
            fontSizeMm = element.fontSizeMm,
            textAlign = textAlignToString(element.textAlign),
            xMm = element.xMm,
            yMm = element.yMm,
            widthMm = element.widthMm,
            heightMm = element.heightMm,
            rotationDeg = element.rotationDeg,
            zIndex = element.zIndex
        )
    }

    private fun fromDto(dto: PageElementDto): PageElement = when (dto) {
        is ImageElementDto -> ImageElement(
            memoryId = dto.memoryId,
            imageUri = dto.imageUri,
            xMm = dto.xMm,
            yMm = dto.yMm,
            widthMm = dto.widthMm,
            heightMm = dto.heightMm,
            rotationDeg = dto.rotationDeg,
            zIndex = dto.zIndex,
            contentScale = dto.contentScale,
            contentOffsetX = dto.contentOffsetX,
            contentOffsetY = dto.contentOffsetY
        )
        is TextElementDto -> TextElement(
            text = dto.text,
            fontSizeMm = dto.fontSizeMm,
            textAlign = stringToTextAlign(dto.textAlign),
            xMm = dto.xMm,
            yMm = dto.yMm,
            widthMm = dto.widthMm,
            heightMm = dto.heightMm,
            rotationDeg = dto.rotationDeg,
            zIndex = dto.zIndex
        )
    }

    private fun textAlignToString(textAlign: TextAlign): String = when (textAlign) {
        TextAlign.Left -> "Left"
        TextAlign.Right -> "Right"
        TextAlign.Center -> "Center"
        TextAlign.Justify -> "Justify"
        TextAlign.Start -> "Start"
        TextAlign.End -> "End"
        else -> "Start"
    }

    private fun stringToTextAlign(value: String): TextAlign = when (value) {
        "Left" -> TextAlign.Left
        "Right" -> TextAlign.Right
        "Center" -> TextAlign.Center
        "Justify" -> TextAlign.Justify
        "Start" -> TextAlign.Start
        "End" -> TextAlign.End
        else -> TextAlign.Start
    }
}

internal fun TextElementDto.toDomain(): TextElement = TextElement(
    text = text,
    fontSizeMm = fontSizeMm,
    textAlign = when (textAlign) {
        "Left" -> TextAlign.Left
        "Right" -> TextAlign.Right
        "Center" -> TextAlign.Center
        "Justify" -> TextAlign.Justify
        "Start" -> TextAlign.Start
        "End" -> TextAlign.End
        else -> TextAlign.Center
    },
    xMm = xMm,
    yMm = yMm,
    widthMm = widthMm,
    heightMm = heightMm,
    rotationDeg = rotationDeg,
    zIndex = zIndex
)
