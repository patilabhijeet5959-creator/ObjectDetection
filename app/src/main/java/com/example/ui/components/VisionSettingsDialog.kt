package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetectionMode
import com.example.data.model.VisionSettings
import com.example.ui.theme.ProfDarkBg
import com.example.ui.theme.ProfOnPrimary
import com.example.ui.theme.ProfPrimary
import com.example.ui.theme.ProfSurfaceContainer
import com.example.ui.theme.ProfSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VisionSettingsDialog(
    settings: VisionSettings,
    onDismiss: () -> Unit,
    onConfidenceThresholdChange: (Float) -> Unit,
    onScanIntervalChange: (Long) -> Unit,
    onToggleLaser: () -> Unit,
    onToggleAudioCues: () -> Unit,
    onDetectionModeChange: (DetectionMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProfDarkBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ProfPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Vision AI Settings",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Confidence Threshold Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Confidence Threshold",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(settings.confidenceThreshold * 100).toInt()}%",
                            color = ProfPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = settings.confidenceThreshold,
                        onValueChange = onConfidenceThresholdChange,
                        valueRange = 0.15f..0.90f,
                        colors = SliderDefaults.colors(
                            thumbColor = ProfPrimary,
                            activeTrackColor = ProfPrimary,
                            inactiveTrackColor = ProfSurfaceVariant
                        ),
                        modifier = Modifier.testTag("confidence_slider")
                    )
                }

                // Scan Speed / Interval
                Column {
                    Text(
                        text = "Scan Interval",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            1000L to "Fast (1.0s)",
                            1800L to "Balanced (1.8s)",
                            3000L to "Battery (3.0s)"
                        ).forEach { (interval, label) ->
                            val isSelected = settings.scanIntervalMs == interval
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ProfPrimary else ProfSurfaceVariant)
                                    .clickable { onScanIntervalChange(interval) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) ProfOnPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Toggle Laser HUD Effect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Laser Scanline Effect",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Subtle sweep animation over live camera feed",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.enableLaserScanEffect,
                        onCheckedChange = { onToggleLaser() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ProfPrimary,
                            checkedTrackColor = ProfPrimary.copy(alpha = 0.35f),
                            uncheckedTrackColor = ProfSurfaceVariant
                        )
                    )
                }

                // Audio Cue on Detection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auditory Feedback",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Play cue when high confidence objects are locked",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.enableAudioCues,
                        onCheckedChange = { onToggleAudioCues() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ProfPrimary,
                            checkedTrackColor = ProfPrimary.copy(alpha = 0.35f),
                            uncheckedTrackColor = ProfSurfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = ProfPrimary)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

