package com.yingjian.feature.photobook.export

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PdfExportFileName {
    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun defaultFileName(
        bookName: String,
        timestampMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val safeName = bookName
            .trim()
            .ifBlank { "画册" }
            .replace(Regex("""[\\/:*?"<>|]+"""), "_")
            .replace(Regex("""\s+"""), "_")
            .trim('_')
            .ifBlank { "画册" }
        val timestamp = Instant.ofEpochMilli(timestampMs)
            .atZone(zoneId)
            .format(timestampFormatter)
        return "${safeName}_$timestamp.pdf"
    }
}
