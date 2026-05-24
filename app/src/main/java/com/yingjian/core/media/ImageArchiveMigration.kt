package com.yingjian.core.media

import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.repository.MemoryRepository
import com.yingjian.core.data.repository.PhotobookRepository
import com.yingjian.core.util.PageLayoutDocumentSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ImageArchiveMigration(
    private val memoryRepository: MemoryRepository,
    private val photobookRepository: PhotobookRepository,
    private val imageArchiveRepository: ImageArchiveRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun migrateReadableImages() {
        val uriMap = mutableMapOf<String, String>()
        migrateMemories(uriMap)
        migratePhotobooks(uriMap)
    }

    private suspend fun migrateMemories(uriMap: MutableMap<String, String>) {
        memoryRepository.getAllMemories().forEach { memory ->
            val oldUris = memory.allImageUris()
            val newUris = oldUris.map { oldUri ->
                archiveIfReadable(oldUri, uriMap)
            }
            if (newUris != oldUris) {
                memoryRepository.updateMemory(
                    memory.copy(
                        imageUri = newUris.firstOrNull() ?: memory.imageUri,
                        imageUrisJson = json.encodeToString(
                            ListSerializer(String.serializer()),
                            newUris
                        )
                    )
                )
            }
        }
    }

    private suspend fun migratePhotobooks(uriMap: MutableMap<String, String>) {
        photobookRepository.getAllPhotobooks().forEach { photobook ->
            photobook.coverImageUri?.let { coverUri ->
                val archivedCoverUri = archiveIfReadable(coverUri, uriMap)
                if (archivedCoverUri != coverUri) {
                    photobookRepository.updatePhotobook(
                        photobook.copy(coverImageUri = archivedCoverUri)
                    )
                }
            }
            photobookRepository.getPageLayouts(photobook.id).forEach { layout ->
                val document = runCatching {
                    PageLayoutDocumentSerializer.deserialize(layout.elementsJson)
                }.getOrNull() ?: return@forEach
                var changed = false
                val updatedSlots = document.slots.map { slot ->
                    val imageRef = slot.imageRef ?: return@map slot
                    val archivedUri = archiveIfReadable(imageRef.imageUri, uriMap)
                    if (archivedUri != imageRef.imageUri) {
                        changed = true
                        slot.copy(imageRef = imageRef.copy(imageUri = archivedUri))
                    } else {
                        slot
                    }
                }
                if (changed) {
                    photobookRepository.savePageLayout(
                        layout.copy(
                            elementsJson = PageLayoutDocumentSerializer.serialize(
                                document.copy(slots = updatedSlots)
                            )
                        )
                    )
                }
            }
        }
    }

    private fun archiveIfReadable(
        uriString: String,
        uriMap: MutableMap<String, String>
    ): String {
        uriMap[uriString]?.let { return it }
        val archivedUri = runCatching {
            imageArchiveRepository.archiveUriString(uriString)
        }.getOrDefault(uriString)
        uriMap[uriString] = archivedUri
        return archivedUri
    }

    private fun MemoryRecordEntity.allImageUris(): List<String> {
        val parsed = runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), imageUrisJson)
        }.getOrDefault(emptyList())
        return parsed.ifEmpty { listOf(imageUri) }
    }
}
