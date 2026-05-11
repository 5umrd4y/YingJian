package com.yingjian.core.data.database

import androidx.room.TypeConverter
import com.yingjian.core.util.ElementSerializer
import com.yingjian.feature.photobook.model.PageElement

class Converters {
    @TypeConverter
    fun fromPageElements(elements: List<PageElement>): String = ElementSerializer.serialize(elements)

    @TypeConverter
    fun toPageElements(json: String): List<PageElement> = ElementSerializer.deserialize(json)
}
