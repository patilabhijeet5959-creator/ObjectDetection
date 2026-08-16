package com.example.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetectedItem
import com.example.ui.theme.ProfDarkBg
import com.example.ui.theme.ProfOnPrimary
import com.example.ui.theme.ProfOnSecondary
import com.example.ui.theme.ProfPrimary
import com.example.ui.theme.ProfSecondary

@Composable
fun BoundingBoxOverlay(
    items: List<DetectedItem>,
    selectedItem: DetectedItem?,
    showLabels: Boolean = true,
    showConfidence: Boolean = true,
    onItemClick: (DetectedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Draw Canvas Rounded Bounding Boxes
        Canvas(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val isSelected = item.id == selectedItem?.id
                val box = item.box
                val left = box.left * widthPx
                val top = box.top * heightPx
                val boxWidth = box.width * widthPx
                val boxHeight = box.height * heightPx
                val categoryColor = if (item.category.name.contains("PERSON") || item.category.name.contains("ELECTRONICS")) {
                    ProfPrimary
                } else {
                    item.category.getColor()
                }

                drawProfessionalBoundingBox(
                    left = left,
                    top = top,
                    width = boxWidth,
                    height = boxHeight,
                    color = categoryColor,
                    isSelected = isSelected,
                    pulseAlpha = if (isSelected) pulseGlow else 0.9f
                )
            }
        }

        // 2. Render Elegant Professional Badge Label on Top-Left of Bounding Box
        if (showLabels) {
            items.forEach { item ->
                val isSelected = item.id == selectedItem?.id
                val box = item.box
                val left = (box.left * widthPx).toInt().coerceAtLeast(8)
                val top = (box.top * heightPx).toInt().coerceAtLeast(8)
                val categoryColor = if (item.category.name.contains("PERSON") || item.category.name.contains("ELECTRONICS")) {
                    ProfPrimary
                } else {
                    item.category.getColor()
                }
                val onBadgeTextColor = if (categoryColor == ProfPrimary) ProfOnPrimary else ProfOnSecondary

                Box(
                    modifier = Modifier
                        .offset { IntOffset(left, top) }
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp))
                        .background(categoryColor.copy(alpha = if (isSelected) 1f else 0.95f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onItemClick(item)
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("bounding_box_chip_${item.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.category.getIcon(),
                            contentDescription = item.category.displayName,
                            tint = onBadgeTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = item.label.uppercase(),
                            color = onBadgeTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        if (showConfidence) {
                            Text(
                                text = " ${(item.confidence * 100).toInt()}%",
                                color = onBadgeTextColor.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawProfessionalBoundingBox(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    isSelected: Boolean,
    pulseAlpha: Float
) {
    if (width <= 0 || height <= 0) return

    val strokeWidth = if (isSelected) 3f else 2f
    val cornerRadius = CornerRadius(14f, 14f)

    // Semi-transparent box fill if selected
    if (isSelected) {
        drawRoundRect(
            color = color.copy(alpha = 0.12f * pulseAlpha),
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = cornerRadius
        )
    }

    // Polished smooth rounded rectangle outline
    drawRoundRect(
        color = color.copy(alpha = pulseAlpha),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = cornerRadius,
        style = Stroke(width = strokeWidth)
    )

    // Center target reticle on selected box
    if (isSelected) {
        val cx = left + width / 2f
        val cy = top + height / 2f
        val reticleSize = 10f
        drawLine(color, Offset(cx - reticleSize, cy), Offset(cx + reticleSize, cy), 1.5f)
        drawLine(color, Offset(cx, cy - reticleSize), Offset(cx, cy + reticleSize), 1.5f)
    }
}

