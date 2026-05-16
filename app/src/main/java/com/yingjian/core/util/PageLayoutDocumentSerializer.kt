package com.yingjian.core.util

import androidx.compose.ui.text.style.TextAlign
import com.yingjian.feature.photobook.model.FitMode
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageLayoutDocument
import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.TextElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class PageLayoutDocumentDto(
    val version: Int = 1,
    val template: String,
    val slots: List<ImageSlotDto>,
    val textElements: List<TextElementDto> = emptyList()
)

@Serializable
private data class ImageSlotDto(
    val slotId: String,
    val imageRef: ImageRefDto?,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val fitMode: String = FitMode.Crop.name,
    val widthMm: Float? = null,
    val heightMm: Float? = null,
    val xMm: Float = 0f,
    val yMm: Float = 0f
)

@Serializable
private data class ImageRefDto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)

object PageLayoutDocumentSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun serialize(document: PageLayoutDocument): String = json.encodeToString(document.toDto())

    fun deserialize(raw: String): PageLayoutDocument = json.decodeFromString<PageLayoutDocumentDto>(raw).toDomain()

    private fun PageLayoutDocument.toDto(): PageLayoutDocumentDto = PageLayoutDocumentDto(
        version = version,
        template = template.name,
        slots = slots.map { it.toDto() },
        textElements = textElements.map { it.toDto() }
    )

    private fun ImageSlot.toDto(): ImageSlotDto = ImageSlotDto(
        slotId = slotId,
        imageRef = imageRef?.let {
            ImageRefDto(
                memoryId = it.memoryId,
                imageUri = it.imageUri,
                sourceImageIndex = it.sourceImageIndex,
                sourceImageId = it.sourceImageId,
                imageWidth = it.imageWidth,
                imageHeight = it.imageHeight
            )
        },
        cropScale = cropScale,
        cropOffsetX = cropOffsetX,
        cropOffsetY = cropOffsetY,
        fitMode = fitMode.name
    )

    private fun PageLayoutDocumentDto.toDomain(): PageLayoutDocument = PageLayoutDocument(
        version = version,
        template = PageTemplate.valueOf(template),
        slots = slots.map { it.toDomain() },
        textElements = textElements.map { it.toDomain() }
    )

    private fun ImageSlotDto.toDomain(): ImageSlot = ImageSlot(
        slotId = slotId,
        imageRef = imageRef?.let {
            ImageRef(
                memoryId = it.memoryId,
                imageUri = it.imageUri,
                sourceImageIndex = it.sourceImageIndex,
                sourceImageId = it.sourceImageId,
                imageWidth = it.imageWidth,
                imageHeight = it.imageHeight
            )
        },
        cropScale = cropScale,
        cropOffsetX = cropOffsetX,
        cropOffsetY = cropOffsetY,
        fitMode = FitMode.valueOf(fitMode)
    )

    private fun TextElement.toDto(): TextElementDto = TextElementDto(
        text = text,
        fontSizeMm = fontSizeMm,
        textAlign = when (textAlign) {
            TextAlign.Left -> "Left"
            TextAlign.Right -> "Right"
            TextAlign.Center -> "Center"
            TextAlign.Justify -> "Justify"
            TextAlign.Start -> "Start"
            TextAlign.End -> "End"
            else -> "Center"
        },
        xMm = xMm,
        yMm = yMm,
        widthMm = widthMm,
        heightMm = heightMm,
        rotationDeg = rotationDeg,
        zIndex = zIndex
    )
}
