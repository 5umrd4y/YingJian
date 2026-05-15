package com.yingjian.feature.photobook

import com.yingjian.core.data.database.MemoryRecordEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class MemoryPhotoPickerMode {
    BatchImport,
    SingleSlot
}

@Serializable
data class SelectedMemoryPhoto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null
)

object SelectedMemoryPhotoCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun encode(value: List<SelectedMemoryPhoto>): String = json.encodeToString(value)
    fun decode(raw: String): List<SelectedMemoryPhoto> = json.decodeFromString(raw)
}

fun MemoryRecordEntity.toSelectablePhotos(): List<SelectedMemoryPhoto> {
    val uris = runCatching {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(imageUrisJson)
    }.getOrDefault(emptyList())
    val sourceUris = if (uris.isEmpty()) listOf(imageUri) else uris
    return sourceUris.mapIndexed { index, uri ->
        SelectedMemoryPhoto(
            memoryId = id,
            imageUri = uri,
            sourceImageIndex = index
        )
    }
}
