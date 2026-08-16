package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VisionUiState
import com.example.ui.VisionViewModel
import com.example.ui.camera.CameraPreviewView
import com.example.ui.components.ObjectInspectorSheet
import com.example.ui.components.VisionControlBar
import com.example.ui.components.VisionHistoryDialog
import com.example.ui.components.VisionSettingsDialog
import com.example.ui.overlay.BoundingBoxOverlay
import com.example.ui.overlay.RadarScannerEffect
import com.example.ui.overlay.SimulationView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProfDarkBg
import com.example.ui.theme.ProfOnPrimary
import com.example.ui.theme.ProfPrimary
import com.example.ui.theme.ProfSecondary
import com.example.ui.theme.ProfSurfaceContainer
import com.example.ui.theme.ProfSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainVisionScreen()
            }
        }
    }
}

@Composable
fun MainVisionScreen(
    viewModel: VisionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        viewModel.setCameraPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.setCameraPermissionGranted(true)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasCameraPermission && !uiState.settings.isSimulationActive) {
                // Permission Request & Simulation fallback Screen
                PermissionRequestContent(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onStartSimulation = { viewModel.toggleSimulation() }
                )
            } else {
                // Live Viewport or Simulated Canvas
                if (uiState.settings.isSimulationActive) {
                    SimulationView(
                        items = uiState.detectedItems,
                        onTapCoordinates = { x, y ->
                            viewModel.onTapToInspect(x, y, uiState.lastCapturedBitmap)
                        }
                    )
                } else {
                    CameraPreviewView(
                        isTorchEnabled = uiState.settings.isTorchEnabled,
                        isFrontCamera = uiState.settings.isFrontCamera,
                        isFrozen = uiState.settings.isFrozen,
                        onFrameCaptured = { bitmap ->
                            viewModel.onFrameCaptured(bitmap)
                        },
                        onTapCoordinates = { x, y ->
                            viewModel.onTapToInspect(x, y, uiState.lastCapturedBitmap)
                        }
                    )
                }

                // 2. AR Bounding Boxes
                BoundingBoxOverlay(
                    items = uiState.detectedItems,
                    selectedItem = uiState.selectedItem,
                    showLabels = uiState.settings.showLabels,
                    showConfidence = uiState.settings.showConfidence,
                    onItemClick = { item -> viewModel.selectItem(item) }
                )

                // 3. Radar Laser Scanline & Top HUD
                RadarScannerEffect(
                    isScanning = uiState.isProcessing,
                    isFrozen = uiState.settings.isFrozen,
                    enableLaser = uiState.settings.enableLaserScanEffect,
                    latencyMs = uiState.detectionLatencyMs,
                    objectCount = uiState.detectedItems.size,
                    detectionMode = uiState.settings.detectionMode,
                    isSimulation = uiState.settings.isSimulationActive,
                    statusMessage = uiState.statusMessage
                )

                // Top Floating History Button
                IconButton(
                    onClick = { viewModel.toggleHistoryDialog(true) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 44.dp, end = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ProfDarkBg.copy(alpha = 0.85f))
                        .border(1.dp, ProfPrimary.copy(alpha = 0.35f), CircleShape)
                        .testTag("open_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = ProfPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Bottom Controls Glassmorphism Bar
                VisionControlBar(
                    settings = uiState.settings,
                    isProcessing = uiState.isProcessing,
                    onTriggerScan = {
                        viewModel.onManualScanTriggered(uiState.lastCapturedBitmap)
                    },
                    onToggleTorch = { viewModel.toggleTorch() },
                    onSwitchCamera = { viewModel.switchCamera() },
                    onTogglePause = { viewModel.togglePause() },
                    onToggleSimulation = { viewModel.toggleSimulation() },
                    onNextScene = { viewModel.nextDemoScene() },
                    onCategoryFilterSelect = { category -> viewModel.setCategoryFilter(category) },
                    onOpenSettings = { viewModel.toggleSettingsDialog(true) },
                    onOpenHistory = { viewModel.toggleHistoryDialog(true) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // 5. Object Inspector Bottom Sheet
            if (uiState.selectedItem != null) {
                ObjectInspectorSheet(
                    selectedItem = uiState.selectedItem,
                    allItems = uiState.detectedItems,
                    isAnalyzingDeep = uiState.isAnalyzingDeep,
                    deepAnalysisText = uiState.deepAnalysisText,
                    onSelectItem = { item -> viewModel.selectItem(item) },
                    onDismiss = { viewModel.selectItem(null) },
                    onAskQuestion = { item, question ->
                        viewModel.requestDeepAnalysis(item, uiState.lastCapturedBitmap, question)
                    }
                )
            }

            // 6. Settings Dialog
            if (uiState.isSettingsOpen) {
                VisionSettingsDialog(
                    settings = uiState.settings,
                    onDismiss = { viewModel.toggleSettingsDialog(false) },
                    onConfidenceThresholdChange = { viewModel.setConfidenceThreshold(it) },
                    onScanIntervalChange = { viewModel.setScanInterval(it) },
                    onToggleLaser = { viewModel.toggleLaserEffect() },
                    onToggleAudioCues = { viewModel.toggleAudioCues() },
                    onDetectionModeChange = { viewModel.setDetectionMode(it) }
                )
            }

            // 7. History Dialog
            if (uiState.isHistoryOpen) {
                VisionHistoryDialog(
                    history = history,
                    onDismiss = { viewModel.toggleHistoryDialog(false) },
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
        }
    }
}

@Composable
fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    onStartSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProfDarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ProfSurfaceContainer)
                .border(1.dp, androidx.compose.ui.graphics.Color(0x1FFFFFFF), RoundedCornerShape(28.dp))
                .padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ProfPrimary.copy(alpha = 0.15f))
                    .border(1.5.dp, ProfPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = ProfPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Live Camera Vision AI",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Allow camera access to identify objects and project real-time bounding boxes onto live video.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProfPrimary,
                    contentColor = ProfOnPrimary
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_camera_permission_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Grant Camera Permission",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onStartSimulation,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ProfSecondary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ProfSecondary.copy(alpha = 0.5f))
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("launch_simulation_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = ProfSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Launch Demo Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
