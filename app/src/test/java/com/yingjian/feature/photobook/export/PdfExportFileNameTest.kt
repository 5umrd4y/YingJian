package com.yingjian.feature.photobook.export

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class PdfExportFileNameTest {
    @Test
    fun defaultFileNameUsesBookNameAndTimestamp() {
        val fileName = PdfExportFileName.defaultFileName(
            bookName = "旅行/画册:2026",
            timestampMs = 1_767_225_600_000L,
            zoneId = ZoneOffset.UTC
        )

        assertEquals("旅行_画册_2026_20260101_000000.pdf", fileName)
    }
}
