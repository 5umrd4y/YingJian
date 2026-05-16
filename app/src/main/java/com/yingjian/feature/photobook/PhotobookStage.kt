package com.yingjian.feature.photobook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults

@Composable
fun PhotobookStage(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 340.dp,
    backgroundColor: Color = Color(PhotobookLayoutDefaults.CONTENT_PAGE_COLOR),
    shadowElevation: Dp = 4.dp,
    cornerRadius: Dp = 4.dp,
    content: @Composable BoxScope.(scaleFactor: Float) -> Unit
) {
    val scaleFactor = maxWidth.value / PhotobookLayoutDefaults.PAGE_WIDTH_MM
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .aspectRatio(PhotobookLayoutDefaults.PAGE_WIDTH_MM / PhotobookLayoutDefaults.PAGE_HEIGHT_MM)
            .shadow(shadowElevation, shape)
            .clip(shape)
            .background(backgroundColor)
    ) {
        content(scaleFactor)
    }
}
