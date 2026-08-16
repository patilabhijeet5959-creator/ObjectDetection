package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ObjectCategory
import com.example.data.model.VisionSettings
import com.example.ui.theme.ProfDarkBg
import com.example.ui.theme.ProfLiveRed
import com.example.ui.theme.ProfOnPrimary
import com.example.ui.theme.ProfPrimary
import com.example.ui.theme.ProfSurfaceContainer
import com.example.ui.theme.ProfSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VisionControlBar(
    settings: VisionSettings,
    isProcessing: Boolean,
    onTriggerScan: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleSimulation: () -> Unit,
    onNextScene: () -> Unit,
    onCategoryFilterSelect: (ObjectCategory?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ProfDarkBg)
            .border(
                width = 1.dp,
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = settings.categoryFilter == null
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) ProfPrimary else ProfSurfaceVariant)
                        .clickable { onCategoryFilterSelect(null) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("filter_all_categories")
                ) {
                    Text(
                        text = "All Items",
                        color = if (isSelected) ProfOnPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            items(ObjectCategory.values()) { category ->
                val isSelected = settings.categoryFilter == category
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) ProfPrimary else ProfSurfaceVariant)
                        .clickable { onCategoryFilterSelect(if (isSelected) null else category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("filter_category_${category.name}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = category.getIcon(),
                            contentDescription = null,
                            tint = if (isSelected) ProfOnPrimary else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = category.displayName,
                            color = if (isSelected) ProfOnPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Action 1: Settings Dialog
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ProfSurfaceVariant)
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Left Action 2: History Log
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ProfSurfaceVariant)
                    .testTag("open_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Detection History",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Center: Primary Inspection / Capture Shutter Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(76.dp)
            ) {
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .border(2.dp, ProfPrimary.copy(alpha = 0.5f), CircleShape)
                    )
                }

                // Outer Ring
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(4.dp, ProfPrimary, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ProfPrimary)
                            .clickable(enabled = !isProcessing) { onTriggerScan() }
                            .testTag("deep_scan_trigger_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ProfOnPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CropFree,
                                contentDescription = "Scan Frame",
                                tint = ProfOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Right Action 1: Torch / Flashlight
            IconButton(
                onClick = onToggleTorch,
                enabled = !settings.isFrontCamera,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (settings.isTorchEnabled) ProfPrimary.copy(alpha = 0.35f) else ProfSurfaceVariant)
                    .testTag("toggle_torch_button")
            ) {
                Icon(
                    imageVector = if (settings.isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flashlight",
                    tint = if (settings.isTorchEnabled) ProfPrimary else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Right Action 2: Flip Camera / Switch
            IconButton(
                onClick = onSwitchCamera,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ProfSurfaceVariant)
                    .testTag("switch_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

