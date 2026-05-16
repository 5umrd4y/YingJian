package com.yingjian.feature.photobook

import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
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
import kotlin.math.roundToInt

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
    val bgColor = try {
        androidx.compose.ui.graphics.Color(Color.parseColor(layout.backgroundColor))
    } catch (_: IllegalArgumentException) {
        androidx.compose.ui.graphics.Color(0xFFAAA194L)
    }

    PhotobookStage(
        modifier = modifier,
        backgroundColor = bgColor
    ) { _ ->
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
                        .pointerInput(element.id) {
                            detectDragGestures(
                                onDragStart = { onTextSelected(element.id) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dxMm = dragAmount.x / density.density / scaleFactor
                                    val dyMm = dragAmount.y / density.density / scaleFactor
                                    val nextX = (element.xMm + dxMm).coerceIn(0f, PhotobookLayoutDefaults.PAGE_WIDTH_MM - element.widthMm)
                                    val nextY = (element.yMm + dyMm).coerceIn(0f, PhotobookLayoutDefaults.PAGE_HEIGHT_MM - element.heightMm)
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
