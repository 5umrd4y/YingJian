package com.yingjian.core.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity

/**
 * Draws a 3mm bleed safety line as a dashed border in screen coordinates.
 * Uses Dp calculation so the on-screen preview matches physical proportions.
 */
@Composable
fun BleedLineOverlay(
    bleedMm: Float = 3f,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bleedDp = with(density) { (bleedMm * scaleFactor).toDp() }
    val bleedPx = with(density) { bleedDp.toPx() }
    val bleedColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 1f
        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        drawRect(
            color = bleedColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                pathEffect = dashPathEffect
            ),
            topLeft = Offset(bleedPx, bleedPx),
            size = Size(
                width = size.width - bleedPx * 2,
                height = size.height - bleedPx * 2
            )
        )
    }
}
