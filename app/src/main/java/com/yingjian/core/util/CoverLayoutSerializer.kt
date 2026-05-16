package com.yingjian.core.util

import com.yingjian.feature.photobook.model.CoverLayout
import com.yingjian.feature.photobook.model.CoverPageType
import com.yingjian.feature.photobook.model.CoverTextElement
import com.yingjian.feature.photobook.model.CoverTextRole
import com.yingjian.feature.photobook.model.PhotobookTextAlign
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class CoverLayoutDto(
    val pageType: String,
    val backgroundColor: String,
    val textElements: List<CoverTextElementDto>
)

@Serializable
private data class CoverTextElementDto(
    val id: String,
    val role: String,
    val text: String,
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float,
    val fontSizeMm: Float,
    val letterSpacing: Float,
    val textAlign: String
)

object CoverLayoutSerializer {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun serialize(layout: CoverLayout): String = json.encodeToString(layout.toDto())

    fun deserialize(raw: String): CoverLayout = json.decodeFromString<CoverLayoutDto>(raw).toDomain()

    private fun CoverLayout.toDto(): CoverLayoutDto = CoverLayoutDto(
        pageType = pageType.name,
        backgroundColor = backgroundColor,
        textElements = textElements.map { it.toDto() }
    )

    private fun CoverTextElement.toDto(): CoverTextElementDto = CoverTextElementDto(
        id = id,
        role = role.name,
        text = text,
        xMm = xMm,
        yMm = yMm,
        widthMm = widthMm,
        heightMm = heightMm,
        fontSizeMm = fontSizeMm,
        letterSpacing = letterSpacing,
        textAlign = textAlign.name
    )

    private fun CoverLayoutDto.toDomain(): CoverLayout = CoverLayout(
        pageType = CoverPageType.valueOf(pageType),
        backgroundColor = backgroundColor,
        textElements = textElements.map { it.toDomain() }
    )

    private fun CoverTextElementDto.toDomain(): CoverTextElement = CoverTextElement(
        id = id,
        role = CoverTextRole.valueOf(role),
        text = text,
        xMm = xMm,
        yMm = yMm,
        widthMm = widthMm,
        heightMm = heightMm,
        fontSizeMm = fontSizeMm,
        letterSpacing = letterSpacing,
        textAlign = PhotobookTextAlign.valueOf(textAlign)
    )
}
