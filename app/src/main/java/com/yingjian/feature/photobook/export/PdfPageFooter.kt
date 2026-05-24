package com.yingjian.feature.photobook.export

import com.yingjian.feature.photobook.model.PageState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class PdfPageFooter(
    val moodText: String?,
    val dateText: String?,
    val pageNumberText: String
) {
    companion object {
        const val TEXT_FONT_SIZE_MM = 4f
        const val PAGE_NUMBER_FONT_SIZE_MM = TEXT_FONT_SIZE_MM

        private val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy.MM.dd")

        fun fromPage(
            page: PageState,
            memoryTimestamps: Map<Long, Long>,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): PdfPageFooter {
            val memoryId = page.slots.firstOrNull { !it.isEmpty }?.imageRef?.memoryId
            val dateText = memoryId
                ?.let(memoryTimestamps::get)
                ?.let { timestamp ->
                    Instant.ofEpochMilli(timestamp).atZone(zoneId).format(dateFormatter)
                }
            return PdfPageFooter(
                moodText = page.textElements.firstOrNull { it.text.isNotBlank() }?.text,
                dateText = dateText,
                pageNumberText = page.pageNumber.toString()
            )
        }
    }
}
