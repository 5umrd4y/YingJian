package com.yingjian.feature.photobook

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun AILayoutLoadingScreen() {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Background: semi-transparent decorative shapes
        Box(modifier = Modifier.fillMaxSize()) {
            repeat(6) { i ->
                Box(
                    modifier = Modifier
                        .size(((i + 1) * 30).dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceVariant.copy(alpha = 0.06f))
                        .rotate((i * 15).toFloat())
                        .graphicsLayer {
                            translationX = (i * 50f) - 100f
                            translationY = (i * 40f) - 80f
                        }
                )
            }
        }

        // Center card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 48.dp, vertical = 64.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "ai_loading")

            // Rotation animation: 8s full rotation
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(8000)),
                label = "rotation"
            )

            // Pulse scale: 4s reverse
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(4000)),
                label = "scale"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(tween(4000)),
                label = "alpha"
            )

            // Center icon area
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Rotating ring
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { rotationZ = rotation }
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        radius = size.minDimension / 2
                    )
                }

                // Pulse ring
                Canvas(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                ) {
                    drawCircle(
                        color = primary.copy(alpha = 0.3f),
                        style = Stroke(width = 2f)
                    )
                }

                // Center text/icon
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.displayLarge
                )
            }

            Text(
                text = "自动布局中",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "正在为您精选照片并排列排版\n请稍候片刻...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
