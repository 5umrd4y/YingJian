package com.yingjian.feature.photobook

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.yingjian.feature.photobook.layout.LayoutInput
import com.yingjian.feature.photobook.layout.SlotImageTransform
import com.yingjian.feature.photobook.layout.SlotRectMm
import com.yingjian.feature.photobook.layout.TemplateLayoutEngine
import com.yingjian.feature.photobook.model.FitMode
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.TextElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotobookCanvasPage(
    pageState: PageState,
    containerWidthDp: Dp,
    moodText: String? = null,
    memoryDate: Long? = null,
    selectedSlotId: String? = null,
    onSlotSelected: (String) -> Unit = {},
    onDeselect: () -> Unit = {},
    onSlotImageAdjusted: (slotId: String, offsetXMm: Float, offsetYMm: Float, scale: Float) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pageAspect = pageState.trimWidthMm / pageState.trimHeightMm
    val scaleFactor = containerWidthDp.value / pageState.trimWidthMm

    val slotRects = remember(pageState.template, pageState.trimWidthMm, pageState.trimHeightMm, pageState.bleedMm) {
        TemplateLayoutEngine.calculateSlots(
            LayoutInput(
                trimWidthMm = pageState.trimWidthMm,
                trimHeightMm = pageState.trimHeightMm,
                bleedMm = pageState.bleedMm,
                safeMarginMm = 16f,
                template = pageState.template
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(pageAspect)
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFAF9F6))
    ) {
        slotRects.forEach { rect ->
            val slot = pageState.slots.firstOrNull { it.slotId == rect.slotId }
            RenderImageSlot(
                rect = rect,
                slot = slot,
                scaleFactor = scaleFactor,
                isSelected = selectedSlotId == rect.slotId,
                onSelect = { onSlotSelected(rect.slotId) },
                onAdjusted = { offsetXMm, offsetYMm, scale ->
                    onSlotImageAdjusted(rect.slotId, offsetXMm, offsetYMm, scale)
                }
            )
        }
        pageState.textElements.forEach { element ->
            RenderPrinterTextElement(
                element = element,
                scaleFactor = scaleFactor,
                dateText = memoryDate?.let { formatDate(it) },
                density = density
            )
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
private fun RenderImageSlot(
    rect: SlotRectMm,
    slot: ImageSlot?,
    scaleFactor: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAdjusted: (offsetXMm: Float, offsetYMm: Float, scale: Float) -> Unit
) {
    val density = LocalDensity.current
    var gestureOffsetX by remember(slot?.slotId, slot?.cropOffsetX) { mutableFloatStateOf(0f) }
    var gestureOffsetY by remember(slot?.slotId, slot?.cropOffsetY) { mutableFloatStateOf(0f) }
    var gestureScale by remember(slot?.slotId, slot?.cropScale) { mutableFloatStateOf(slot?.cropScale ?: 1f) }

    fun pxToMm(px: Float): Float = with(density) { px / scaleFactor / density.density }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (rect.xMm * scaleFactor).dp.roundToPx(),
                    (rect.yMm * scaleFactor).dp.roundToPx()
                )
            }
            .size((rect.widthMm * scaleFactor).dp, (rect.heightMm * scaleFactor).dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(2.dp)
            )
            .clickable(onClick = onSelect)
            .pointerInput(isSelected) {
                if (isSelected && slot?.imageRef != null) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        gestureOffsetX += pan.x
                        gestureOffsetY += pan.y
                        gestureScale = SlotImageTransform.clampScale(gestureScale * zoom)
                        val offsetXMm = SlotImageTransform.clampOffset((slot.cropOffsetX + pxToMm(gestureOffsetX)), rect.widthMm, gestureScale)
                        val offsetYMm = SlotImageTransform.clampOffset((slot.cropOffsetY + pxToMm(gestureOffsetY)), rect.heightMm, gestureScale)
                        onAdjusted(offsetXMm, offsetYMm, gestureScale)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (slot?.imageRef == null) {
            Text("+", color = MaterialTheme.colorScheme.outline)
        } else {
            AsyncImage(
                model = Uri.parse(slot.imageRef.imageUri),
                contentDescription = null,
                contentScale = if (slot.fitMode == FitMode.Crop) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = slot.cropScale
                        scaleY = slot.cropScale
                        translationX = with(density) { (slot.cropOffsetX * scaleFactor).dp.toPx() }
                        translationY = with(density) { (slot.cropOffsetY * scaleFactor).dp.toPx() }
                    }
            )
        }
    }
}

@Composable
private fun RenderPrinterTextElement(
    element: TextElement,
    scaleFactor: Float,
    dateText: String? = null,
    density: androidx.compose.ui.unit.Density
) {
    with(density) {
        val xPx = (element.xMm * scaleFactor).dp.roundToPx()
        val yPx = (element.yMm * scaleFactor).dp.roundToPx()
        val widthDp = ((element.widthMm * scaleFactor)).dp

        Row(
            modifier = Modifier
                .offset { IntOffset(xPx, yPx) }
                .width(widthDp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = element.text,
                fontSize = (11f * scaleFactor).sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = (0.2f * scaleFactor).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
            if (dateText != null) {
                Text(
                    text = dateText,
                    fontSize = (9f * scaleFactor).sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (0.1f * scaleFactor).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(timestamp))
