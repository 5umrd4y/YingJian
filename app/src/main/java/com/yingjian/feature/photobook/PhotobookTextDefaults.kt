package com.yingjian.feature.photobook

import com.yingjian.core.data.database.PhotobookEntity

fun PhotobookEntity.displayCoverTitle(): String =
    coverTitle?.takeIf { it.isNotBlank() } ?: name

fun PhotobookEntity.displayCoverSubtitle(): String =
    coverSubtitle?.takeIf { it.isNotBlank() } ?: "/ HAOS"

fun PhotobookEntity.displayBackTitle(): String =
    backTitle?.takeIf { it.isNotBlank() } ?: "《${displayCoverTitle()}》"

fun PhotobookEntity.displayBackSubtitle(): String =
    backSubtitle?.takeIf { it.isNotBlank() } ?: "All Photo by HAOS"

fun PhotobookEntity.displayBackDateText(): String =
    backDateText?.takeIf { it.isNotBlank() } ?: ""
