package com.yingjian.core.util

import com.yingjian.feature.photobook.model.CoverLayoutDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverLayoutSerializerTest {
    @Test
    fun `cover layout round trips through json`() {
        val layout = CoverLayoutDefaults.defaultBackCover(
            title = "Back",
            subtitle = "Subtitle",
            dateText = "2026.05.16"
        )

        val json = CoverLayoutSerializer.serialize(layout)
        val decoded = CoverLayoutSerializer.deserialize(json)

        assertEquals(layout, decoded)
    }
}
