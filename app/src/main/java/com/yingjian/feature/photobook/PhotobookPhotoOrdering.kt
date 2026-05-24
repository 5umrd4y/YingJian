package com.yingjian.feature.photobook

import com.yingjian.core.data.database.MemoryRecordEntity

object PhotobookPhotoOrdering {
    fun byMemoryDateAscending(
        photos: List<SelectedMemoryPhoto>,
        memoryById: Map<Long, MemoryRecordEntity>
    ): List<SelectedMemoryPhoto> =
        photos.withIndex()
            .sortedWith(
                compareBy<IndexedValue<SelectedMemoryPhoto>>(
                    { memoryById[it.value.memoryId]?.timestamp ?: Long.MAX_VALUE },
                    { it.index }
                )
            )
            .map { it.value }
}
