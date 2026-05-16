package com.yingjian.feature.photobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yingjian.feature.photobook.model.CoverLayout
import com.yingjian.feature.photobook.model.CoverTextRole
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults
import com.yingjian.feature.photobook.model.toComposeTextAlign

@Composable
fun CoverPageRenderer(
    layout: CoverLayout,
    scaleFactor: Float,
    selectedTextId: String?,
    onTextSelected: (String) -> Unit,
    onTextMoved: (textId: String, xMm: Float, yMm: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        layout.textElements.forEach { element ->
            val isSelected = selectedTextId == element.id
            if (element.role == CoverTextRole.Divider) {
                Canvas(
                    modifier = Modifier
                        .offset {
                            with(density) {
                                IntOffset((element.xMm * scaleFactor).dp.roundToPx(), (element.yMm * scaleFactor).dp.roundToPx())
                            }
                        }
                        .size((element.widthMm * scaleFactor).dp, (element.heightMm * scaleFactor).dp)
                ) {
                    drawLine(
                        color = androidx.compose.ui.graphics.Color(0xFF4C463EL),
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = size.height.coerceAtLeast(1f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .offset {
                            with(density) {
                                IntOffset((element.xMm * scaleFactor).dp.roundToPx(), (element.yMm * scaleFactor).dp.roundToPx())
                            }
                        }
                        .size((element.widthMm * scaleFactor).dp, (element.heightMm * scaleFactor).dp)
                        .then(if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary) else Modifier)
                        .pointerInput(element.id, element.xMm, element.yMm) {
                            var dragAccumulatorX = 0f
                            var dragAccumulatorY = 0f
                            val startXMm = element.xMm
                            val startYMm = element.yMm
                            detectDragGestures(
                                onDragStart = {
                                    dragAccumulatorX = 0f
                                    dragAccumulatorY = 0f
                                    onTextSelected(element.id)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccumulatorX += dragAmount.x
                                    dragAccumulatorY += dragAmount.y
                                    val dxMm = dragAccumulatorX / density.density / scaleFactor
                                    val dyMm = dragAccumulatorY / density.density / scaleFactor
                                    val nextX = (startXMm + dxMm).coerceIn(0f, PhotobookLayoutDefaults.PAGE_WIDTH_MM - element.widthMm)
                                    val nextY = (startYMm + dyMm).coerceIn(0f, PhotobookLayoutDefaults.PAGE_HEIGHT_MM - element.heightMm)
                                    onTextMoved(element.id, nextX, nextY)
                                }
                            )
                        }
                ) {
                    Text(
                        text = element.text,
                        fontSize = (element.fontSizeMm * scaleFactor).sp,
                        fontWeight = FontWeight.Light,
                        textAlign = element.textAlign.toComposeTextAlign(),
                        color = androidx.compose.ui.graphics.Color(0xFF4C463EL)
                    )
                }
            }
        }
    }
}
