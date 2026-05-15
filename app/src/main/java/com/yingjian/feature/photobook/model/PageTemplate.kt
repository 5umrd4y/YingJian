package com.yingjian.feature.photobook.model

enum class PageTemplate(val slotIds: List<String>) {
    Single(listOf("slot-1")),
    TwoHorizontal(listOf("slot-1", "slot-2")),
    TwoVertical(listOf("slot-1", "slot-2")),
    GridFour(listOf("slot-1", "slot-2", "slot-3", "slot-4"));

    val capacity: Int get() = slotIds.size
}
