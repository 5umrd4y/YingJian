package com.yingjian.feature.photobook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import com.yingjian.core.ui.common.BleedLineOverlay
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.PageElement
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.TextElement

/**
 * Renders a single photobook page for the editor.
 * scaleFactor = containerWidthDp / pageState.trimWidthMm (Dp per mm).
 */
@Composable
fun PhotobookCanvasPage(
    pageState: PageState,
    containerWidthDp: Dp,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pageAspect = pageState.trimWidthMm / pageState.trimHeightMm

    // Dp per mm: how many screen Dp represent 1mm of physical page
    val scaleFactor = containerWidthDp.value / pageState.trimWidthMm

    Box(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(pageAspect)
            .shadow(8.dp, shape = MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            pageState.elements.forEach { element ->
                when (element) {
                    is ImageElement -> RenderImageElement(element, scaleFactor, zoomLevel)
                    is TextElement -> RenderTextElement(element, scaleFactor, zoomLevel)
                }
            }

            BleedLineOverlay(
                bleedMm = pageState.bleedMm,
                scaleFactor = scaleFactor
            )
        }
    }
}

@Composable
private fun RenderImageElement(
    element: ImageElement,
    scaleFactor: Float,
    zoomLevel: Float
) {
    val xDp = (element.xMm * scaleFactor * zoomLevel)
    val yDp = (element.yMm * scaleFactor * zoomLevel)
    val wDp = (element.widthMm * scaleFactor * zoomLevel)
    val hDp = (element.heightMm * scaleFactor * zoomLevel)

    AsyncImage(
        model = element.imageUri,
        contentDescription = null,
        modifier = Modifier
            .offset { IntOffset(xDp.roundToInt(), yDp.roundToInt()) }
            .size(wDp.dp, hDp.dp)
    )
}

@Composable
private fun RenderTextElement(
    element: TextElement,
    scaleFactor: Float,
    zoomLevel: Float
) {
    val xDp = (element.xMm * scaleFactor * zoomLevel)
    val yDp = (element.yMm * scaleFactor * zoomLevel)

    Text(
        text = element.text,
        fontSize = (element.fontSizeMm * scaleFactor * zoomLevel).sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.offset { IntOffset(xDp.roundToInt(), yDp.roundToInt()) }
    )
}
