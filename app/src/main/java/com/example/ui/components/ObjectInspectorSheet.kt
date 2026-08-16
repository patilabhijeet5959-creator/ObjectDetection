package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetectedItem
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ProfDarkBg
import com.example.ui.theme.ProfOnPrimary
import com.example.ui.theme.ProfPrimary
import com.example.ui.theme.ProfSecondary
import com.example.ui.theme.ProfSurfaceContainer
import com.example.ui.theme.ProfSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectInspectorSheet(
    selectedItem: DetectedItem?,
    allItems: List<DetectedItem>,
    isAnalyzingDeep: Boolean,
    deepAnalysisText: String?,
    onSelectItem: (DetectedItem) -> Unit,
    onDismiss: () -> Unit,
    onAskQuestion: (DetectedItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedItem == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customQuestion by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ProfDarkBg,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Category Icon, Label and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ProfPrimary.copy(alpha = 0.2f))
                            .border(1.dp, ProfPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedItem.category.getIcon(),
                            contentDescription = selectedItem.category.displayName,
                            tint = ProfPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = selectedItem.label,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedItem.category.displayName.uppercase(),
                            color = ProfPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ProfSurfaceVariant)
                        .testTag("close_inspector_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Other items in view carousel
            if (allItems.size > 1) {
                Text(
                    text = "ALL DETECTED ITEMS (${allItems.size})",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allItems) { item ->
                        val isCurrent = item.id == selectedItem.id
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isCurrent) ProfPrimary else ProfSurfaceVariant)
                                .clickable { onSelectItem(item) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = item.category.getIcon(),
                                    contentDescription = null,
                                    tint = if (isCurrent) ProfOnPrimary else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = item.label,
                                    color = if (isCurrent) ProfOnPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Confidence Bar & Coordinates Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ProfSurfaceContainer)
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Detection Confidence",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(selectedItem.confidence * 100).toInt()}%",
                            color = ProfPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { selectedItem.confidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ProfPrimary,
                        trackColor = ProfSurfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bounding Box Telemetry
                    val box = selectedItem.box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Bounding Box Area",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${(box.width * 100).toInt()}% W × ${(box.height * 100).toInt()}% H",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column {
                            Text(
                                text = "Normalized Coordinates",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "[${(box.top * 100).toInt()}, ${(box.left * 100).toInt()}, ${(box.bottom * 100).toInt()}, ${(box.right * 100).toInt()}]",
                                color = ProfSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Deep Insights Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ProfSurfaceContainer)
                    .border(1.dp, ProfPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Analysis",
                            tint = ProfPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Vision AI Analysis",
                            color = ProfPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isAnalyzingDeep) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ProfPrimary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Analyzing visual features and context with Gemini...",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    } else {
                        Text(
                            text = deepAnalysisText
                                ?: selectedItem.description.ifBlank { "Object recognized within target boundaries with high fidelity." },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ask AI Quick Prompt or custom question
            Text(
                text = "ASK VISION AI ABOUT THIS OBJECT",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Preset Quick Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("What is it used for?", "Material & Specs", "Estimated size").forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ProfSurfaceVariant)
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                            .clickable { onAskQuestion(selectedItem, prompt) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prompt,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Question Input
            OutlinedTextField(
                value = customQuestion,
                onValueChange = { customQuestion = it },
                placeholder = { Text("Ask anything about this ${selectedItem.label}...", color = TextMuted, fontSize = 13.sp) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (customQuestion.isNotBlank()) {
                                onAskQuestion(selectedItem, customQuestion)
                                customQuestion = ""
                            }
                        },
                        enabled = customQuestion.isNotBlank(),
                        modifier = Modifier.testTag("send_question_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (customQuestion.isNotBlank()) ProfPrimary else TextMuted
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProfPrimary,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedContainerColor = ProfSurfaceContainer,
                    unfocusedContainerColor = ProfSurfaceContainer,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

