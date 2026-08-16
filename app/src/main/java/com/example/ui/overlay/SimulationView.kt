package com.example.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.DetectedItem
import com.example.ui.theme.ProfDarkBg
import com.example.ui.theme.ProfPrimary
import com.example.ui.theme.ProfSurfaceContainer

@Composable
fun SimulationView(
    items: List<DetectedItem>,
    onTapCoordinates: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gridMove")
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProfDarkBg)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = offset.x / size.width.toFloat()
                    val normY = offset.y / size.height.toFloat()
                    onTapCoordinates(normX, normY)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Radial gradient background
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ProfSurfaceContainer.copy(alpha = 0.8f), ProfDarkBg),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.9f
                )
            )

            // Grid lines
            val step = 60f
            val gridColor = ProfPrimary.copy(alpha = 0.08f)

            var x = (gridOffset % step)
            while (x < w) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), 1f)
                x += step
            }

            var y = (gridOffset % step)
            while (y < h) {
                drawLine(gridColor, Offset(0f, y), Offset(w, y), 1f)
                y += step
            }

            // Draw subtle wireframe shapes representing simulated detected objects
            items.forEach { item ->
                val box = item.box
                val left = box.left * w
                val top = box.top * h
                val width = box.width * w
                val height = box.height * h
                val color = item.category.getColor()

                // Semi-transparent object silhouette
                drawRoundRect(
                    color = color.copy(alpha = 0.12f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )

                drawRoundRect(
                    color = color.copy(alpha = 0.35f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                )
            }
        }
    }
}

