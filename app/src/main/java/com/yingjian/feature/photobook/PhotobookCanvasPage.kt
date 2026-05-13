package com.yingjian.feature.photobook

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.PageElement
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.TextElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PhotobookCanvasPage(
    pageState: PageState,
    containerWidthDp: Dp,
    moodText: String? = null,
    memoryDate: Long? = null,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onDeselect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pageAspect = pageState.trimWidthMm / pageState.trimHeightMm
    val scaleFactor = containerWidthDp.value / pageState.trimWidthMm

    // Gesture state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(pageAspect)
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFAF9F6))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (isSelected) onDeselect() else onSelect()
                    },
                    onTap = {
                        if (isSelected) onDeselect()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offsetX += pan.x
                    offsetY += pan.y
                    scale *= zoom
                }
            }
    ) {
        // Render page elements with gestural offset/scale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            // Format date from memory timestamp
            val dateText = memoryDate?.let { formatDate(it) }

            pageState.elements.forEach { element ->
                when (element) {
                    is ImageElement -> RenderImageElement(
                        element = element,
                        scaleFactor = scaleFactor,
                        zoomLevel = scale
                    )
                    is TextElement -> RenderPrinterTextElement(
                        element = element,
                        scaleFactor = scaleFactor,
                        dateText = dateText
                    )
                }
            }

            // If no TextElement but moodText exists, render them below the image
            if (moodText != null && pageState.elements.none { it is TextElement }) {
                val imageEl = pageState.elements.filterIsInstance<ImageElement>().firstOrNull()
                if (imageEl != null) {
                    val yDp = ((imageEl.yMm + imageEl.heightMm + 8f) * scaleFactor)
                    val xDp = ((imageEl.xMm + imageEl.widthMm / 2f) * scaleFactor)
                    val widthDp = (imageEl.widthMm * scaleFactor)

                    Column(
                        modifier = Modifier
                            .offset { IntOffset((xDp.roundToInt() - (widthDp / 2).dp.roundToPx()), yDp.roundToInt()) }
                            .width(widthDp.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = moodText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraLight,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (dateText != null) {
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraLight,
                                    letterSpacing = 0.1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Selection border
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
            // Corner handles
            listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
                .forEach { alignment ->
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .offset(
                                x = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart) (-4).dp else 4.dp,
                                y = if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd) (-4).dp else 4.dp
                            )
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
        }

        // Page number (bottom-right)
        Text(
            text = "${pageState.pageNumber}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 0.2.sp
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        )
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
        model = Uri.parse(element.imageUri),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .offset { IntOffset(xDp.roundToInt(), yDp.roundToInt()) }
            .size(wDp.dp, hDp.dp)
    )
}

@Composable
private fun RenderPrinterTextElement(
    element: TextElement,
    scaleFactor: Float,
    dateText: String? = null
) {
    val xDp = (element.xMm * scaleFactor)
    val yDp = (element.yMm * scaleFactor)
    val widthDp = (element.widthMm * scaleFactor)

    Column(
        modifier = Modifier
            .offset { IntOffset(xDp.roundToInt(), yDp.roundToInt()) }
            .width(widthDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = element.text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 0.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (dateText != null) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 0.1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(timestamp))
