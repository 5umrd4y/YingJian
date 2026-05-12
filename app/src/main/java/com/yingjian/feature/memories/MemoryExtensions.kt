package com.yingjian.feature.memories

import com.yingjian.core.data.database.MemoryRecordEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Returns the full list of image URIs for a memory.
 * If imageUrisJson is non-empty and valid, deserializes it;
 * otherwise falls back to [imageUri].
 */
fun MemoryRecordEntity.getAllImageUris(): List<String> {
    return if (imageUrisJson.isNotBlank() && imageUrisJson != "[]") {
        runCatching {
            Json.decodeFromString(ListSerializer(String.serializer()), imageUrisJson)
        }.getOrNull() ?: listOf(imageUri)
    } else {
        listOf(imageUri)
    }
}
