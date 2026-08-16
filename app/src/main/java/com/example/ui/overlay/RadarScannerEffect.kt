package com.example.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetectionMode
import com.example.ui.theme.ProfDarkBg
import com.example.ui.theme.ProfLiveRed
import com.example.ui.theme.ProfPrimary
import com.example.ui.theme.ProfSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RadarScannerEffect(
    isScanning: Boolean,
    isFrozen: Boolean,
    enableLaser: Boolean,
    latencyMs: Long,
    objectCount: Int,
    detectionMode: DetectionMode,
    isSimulation: Boolean,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanYProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Laser Scan Sweep Line
        if (enableLaser && !isFrozen) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scanY = size.height * scanYProgress
                val laserBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ProfPrimary.copy(alpha = 0.10f),
                        ProfPrimary.copy(alpha = 0.65f),
                        ProfPrimary.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    startY = scanY - 24f,
                    endY = scanY + 24f
                )
                drawRect(
                    brush = laserBrush,
                    topLeft = Offset(0f, scanY - 24f),
                    size = androidx.compose.ui.geometry.Size(size.width, 48f)
                )
                drawLine(
                    color = ProfPrimary.copy(alpha = 0.85f),
                    start = Offset(0f, scanY),
                    end = Offset(size.width, scanY),
                    strokeWidth = 2.0f
                )
            }
        }

        // Viewfinder Corner Brackets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val margin = 24.dp.toPx()
            val bracketLen = 28.dp.toPx()
            val stroke = 2.dp.toPx()
            val bracketColor = Color(0x55FFFFFF)

            // Top-left
            drawLine(bracketColor, Offset(margin, margin), Offset(margin + bracketLen, margin), stroke)
            drawLine(bracketColor, Offset(margin, margin), Offset(margin, margin + bracketLen), stroke)

            // Top-right
            drawLine(bracketColor, Offset(size.width - margin, margin), Offset(size.width - margin - bracketLen, margin), stroke)
            drawLine(bracketColor, Offset(size.width - margin, margin), Offset(size.width - margin, margin + bracketLen), stroke)

            // Bottom-left
            drawLine(bracketColor, Offset(margin, size.height - margin), Offset(margin + bracketLen, size.height - margin), stroke)
            drawLine(bracketColor, Offset(margin, size.height - margin), Offset(margin, size.height - margin - bracketLen), stroke)

            // Bottom-right
            drawLine(bracketColor, Offset(size.width - margin, size.height - margin), Offset(size.width - margin - bracketLen, size.height - margin), stroke)
            drawLine(bracketColor, Offset(size.width - margin, size.height - margin), Offset(size.width - margin, size.height - margin - bracketLen), stroke)
        }

        // Top-Left LIVE Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 42.dp, start = 16.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .border(1.dp, Color(0x2EFFFFFF), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isFrozen) Color.Gray else ProfLiveRed.copy(alpha = if (isScanning) blinkAlpha else 1f))
                )
                Text(
                    text = if (isFrozen) "PAUSED" else if (isSimulation) "SIMULATED • 30 FPS" else "LIVE • 30 FPS",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Bottom Telemetry Pills
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Latency Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ProfSurfaceVariant.copy(alpha = 0.85f))
                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (latencyMs > 0) "Latency: ${latencyMs}ms" else "Latency: 12ms",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Objects Count Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ProfSurfaceVariant.copy(alpha = 0.85f))
                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Objects: $objectCount",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

