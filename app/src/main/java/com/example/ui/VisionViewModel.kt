package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DetectedItem
import com.example.data.model.DetectionMode
import com.example.data.model.NormalizedRect
import com.example.data.model.ObjectCategory
import com.example.data.model.SavedDetectionSession
import com.example.data.model.VisionSettings
import com.example.data.repository.VisionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VisionUiState(
    val detectedItems: List<DetectedItem> = emptyList(),
    val rawDetectedItems: List<DetectedItem> = emptyList(),
    val selectedItem: DetectedItem? = null,
    val isProcessing: Boolean = false,
    val detectionLatencyMs: Long = 0L,
    val fps: Int = 30,
    val settings: VisionSettings = VisionSettings(),
    val isCameraPermissionGranted: Boolean = false,
    val deepAnalysisText: String? = null,
    val isAnalyzingDeep: Boolean = false,
    val currentDemoSceneIndex: Int = 0,
    val statusMessage: String = "AI Vision Ready",
    val lastCapturedBitmap: Bitmap? = null,
    val isHistoryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false
)

class VisionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VisionRepository()

    private val _uiState = MutableStateFlow(VisionUiState())
    val uiState: StateFlow<VisionUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<SavedDetectionSession>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var continuousScanJob: Job? = null
    private var lastAnalysisTimestamp = 0L
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
        } catch (e: Exception) {
            // ToneGenerator not supported on some virtual devices
        }
        startSimulationLoopIfNeeded()
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isCameraPermissionGranted = granted) }
    }

    fun onFrameCaptured(bitmap: Bitmap) {
        val state = _uiState.value
        if (state.settings.isFrozen || state.settings.isSimulationActive) return

        val now = System.currentTimeMillis()
        if (now - lastAnalysisTimestamp < state.settings.scanIntervalMs) {
            return
        }
        if (state.isProcessing) return

        lastAnalysisTimestamp = now
        _uiState.update { it.copy(lastCapturedBitmap = bitmap) }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Scanning frame...") }
            val startTime = System.currentTimeMillis()
            val result = repository.analyzeFrame(bitmap)
            val latency = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val rawItems = result.getOrNull() ?: emptyList()
                val filtered = filterItems(rawItems, state.settings)
                val newItemsFound = rawItems.size > state.rawDetectedItems.size

                if (newItemsFound && state.settings.enableAudioCues) {
                    playBeepCue()
                }

                _uiState.update {
                    it.copy(
                        rawDetectedItems = rawItems,
                        detectedItems = filtered,
                        isProcessing = false,
                        detectionLatencyMs = latency,
                        statusMessage = if (filtered.isEmpty()) "Scanning scene (0 objects)" else "Identified ${filtered.size} object(s)"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Vision tracking active"
                    )
                }
            }
        }
    }

    fun onManualScanTriggered(bitmap: Bitmap?) {
        val bmp = bitmap ?: _uiState.value.lastCapturedBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Deep analyzing frame...") }
            val startTime = System.currentTimeMillis()
            val result = repository.analyzeFrame(bmp)
            val latency = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val rawItems = result.getOrNull() ?: emptyList()
                val filtered = filterItems(rawItems, _uiState.value.settings)
                _uiState.update {
                    it.copy(
                        rawDetectedItems = rawItems,
                        detectedItems = filtered,
                        isProcessing = false,
                        detectionLatencyMs = latency,
                        statusMessage = "Deep scan: ${filtered.size} objects found"
                    )
                }
                repository.saveSession(filtered, "Deep Scan", latency)
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Deep scan completed"
                    )
                }
            }
        }
    }

    fun onTapToInspect(normalizedX: Float, normalizedY: Float, currentBitmap: Bitmap?) {
        val state = _uiState.value
        // First check if user tapped on an existing bounding box
        val clickedItem = state.detectedItems.find { item ->
            normalizedX >= item.box.left && normalizedX <= item.box.right &&
                    normalizedY >= item.box.top && normalizedY <= item.box.bottom
        }

        if (clickedItem != null) {
            selectItem(clickedItem)
            return
        }

        // Otherwise, run targeted spot analysis
        val bmp = currentBitmap ?: state.lastCapturedBitmap
        if (bmp != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isProcessing = true, statusMessage = "Analyzing tapped target...") }
                val startTime = System.currentTimeMillis()
                val result = repository.analyzeFrame(bmp, focusPoint = Pair(normalizedX, normalizedY))
                val latency = System.currentTimeMillis() - startTime

                if (result.isSuccess) {
                    val rawItems = result.getOrNull() ?: emptyList()
                    val filtered = filterItems(rawItems, state.settings)
                    val targetItem = filtered.firstOrNull()

                    _uiState.update {
                        it.copy(
                            rawDetectedItems = it.rawDetectedItems + rawItems,
                            detectedItems = it.detectedItems + filtered,
                            selectedItem = targetItem,
                            isProcessing = false,
                            detectionLatencyMs = latency,
                            statusMessage = targetItem?.let { item -> "Locked target: ${item.label}" } ?: "Target analyzed"
                        )
                    }
                    if (targetItem != null) {
                        requestDeepAnalysis(targetItem, bmp)
                    }
                } else {
                    _uiState.update { it.copy(isProcessing = false) }
                }
            }
        }
    }

    fun selectItem(item: DetectedItem?) {
        _uiState.update {
            it.copy(
                selectedItem = item,
                deepAnalysisText = null
            )
        }
        if (item != null) {
            val bmp = _uiState.value.lastCapturedBitmap
            if (bmp != null) {
                requestDeepAnalysis(item, bmp)
            }
        }
    }

    fun requestDeepAnalysis(item: DetectedItem, bitmap: Bitmap?, customQuestion: String? = null) {
        val bmp = bitmap ?: _uiState.value.lastCapturedBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingDeep = true) }
            val result = repository.getDeepAnalysis(bmp, item, customQuestion)
            _uiState.update {
                it.copy(
                    isAnalyzingDeep = false,
                    deepAnalysisText = result.getOrDefault("Analysis unavailable.")
                )
            }
        }
    }

    fun toggleTorch() {
        _uiState.update {
            val newSettings = it.settings.copy(isTorchEnabled = !it.settings.isTorchEnabled)
            it.copy(settings = newSettings)
        }
    }

    fun switchCamera() {
        _uiState.update {
            val newSettings = it.settings.copy(isFrontCamera = !it.settings.isFrontCamera)
            it.copy(settings = newSettings)
        }
    }

    fun togglePause() {
        _uiState.update {
            val newFrozen = !it.settings.isFrozen
            val newSettings = it.settings.copy(isFrozen = newFrozen)
            it.copy(
                settings = newSettings,
                statusMessage = if (newFrozen) "Camera Frame Frozen (Paused)" else "Live Vision Active"
            )
        }
    }

    fun toggleSimulation() {
        val isSimActive = !_uiState.value.settings.isSimulationActive
        _uiState.update {
            val newSettings = it.settings.copy(isSimulationActive = isSimActive)
            it.copy(
                settings = newSettings,
                statusMessage = if (isSimActive) "Simulation Mode: ${repository.demoScenes.first().name}" else "Live Camera Feed Active"
            )
        }
        if (isSimActive) {
            startSimulationLoop()
        } else {
            continuousScanJob?.cancel()
        }
    }

    fun nextDemoScene() {
        val nextIdx = (_uiState.value.currentDemoSceneIndex + 1) % repository.demoScenes.size
        _uiState.update {
            it.copy(
                currentDemoSceneIndex = nextIdx,
                statusMessage = "Scene: ${repository.demoScenes[nextIdx].name}"
            )
        }
        updateSimulationFrame()
    }

    fun setConfidenceThreshold(threshold: Float) {
        _uiState.update {
            val newSettings = it.settings.copy(confidenceThreshold = threshold)
            val filtered = filterItems(it.rawDetectedItems, newSettings)
            it.copy(
                settings = newSettings,
                detectedItems = filtered
            )
        }
    }

    fun setCategoryFilter(category: ObjectCategory?) {
        _uiState.update {
            val newSettings = it.settings.copy(categoryFilter = category)
            val filtered = filterItems(it.rawDetectedItems, newSettings)
            it.copy(
                settings = newSettings,
                detectedItems = filtered
            )
        }
    }

    fun setDetectionMode(mode: DetectionMode) {
        _uiState.update {
            val newSettings = it.settings.copy(detectionMode = mode)
            it.copy(
                settings = newSettings,
                statusMessage = "Mode: ${mode.name}"
            )
        }
    }

    fun toggleLaserEffect() {
        _uiState.update {
            val newSettings = it.settings.copy(enableLaserScanEffect = !it.settings.enableLaserScanEffect)
            it.copy(settings = newSettings)
        }
    }

    fun toggleAudioCues() {
        _uiState.update {
            val newSettings = it.settings.copy(enableAudioCues = !it.settings.enableAudioCues)
            it.copy(settings = newSettings)
        }
    }

    fun setScanInterval(intervalMs: Long) {
        _uiState.update {
            val newSettings = it.settings.copy(scanIntervalMs = intervalMs)
            it.copy(settings = newSettings)
        }
    }

    fun toggleHistoryDialog(open: Boolean) {
        _uiState.update { it.copy(isHistoryOpen = open) }
    }

    fun toggleSettingsDialog(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun saveCurrentSnapshot() {
        val state = _uiState.value
        val items = state.detectedItems
        val sceneName = if (state.settings.isSimulationActive) {
            repository.demoScenes.getOrNull(state.currentDemoSceneIndex)?.name ?: "Simulated View"
        } else {
            "Live Camera Scan"
        }
        repository.saveSession(items, sceneName, state.detectionLatencyMs)
        _uiState.update { it.copy(statusMessage = "Saved snapshot (${items.size} objects)") }
    }

    fun clearHistory() {
        repository.clearHistory()
    }

    private fun filterItems(items: List<DetectedItem>, settings: VisionSettings): List<DetectedItem> {
        return items.filter { item ->
            item.confidence >= settings.confidenceThreshold &&
                    (settings.categoryFilter == null || item.category == settings.categoryFilter)
        }
    }

    private fun startSimulationLoopIfNeeded() {
        // If simulation becomes active
        if (_uiState.value.settings.isSimulationActive) {
            startSimulationLoop()
        }
    }

    private fun startSimulationLoop() {
        continuousScanJob?.cancel()
        continuousScanJob = viewModelScope.launch {
            while (isActive && _uiState.value.settings.isSimulationActive) {
                if (!_uiState.value.settings.isFrozen) {
                    updateSimulationFrame()
                }
                delay(800)
            }
        }
    }

    private fun updateSimulationFrame() {
        val state = _uiState.value
        val simulatedItems = repository.getSimulatedSceneItems(state.currentDemoSceneIndex)
        val filtered = filterItems(simulatedItems, state.settings)
        val simBitmap = repository.createSimulatedBitmap(simulatedItems)

        _uiState.update {
            it.copy(
                rawDetectedItems = simulatedItems,
                detectedItems = filtered,
                lastCapturedBitmap = simBitmap,
                detectionLatencyMs = 120L + (kotlin.random.Random.nextLong(60))
            )
        }
    }

    private fun playBeepCue() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        } catch (e: Exception) {
            // Ignore sound error
        }
    }

    override fun onCleared() {
        super.onCleared()
        continuousScanJob?.cancel()
        toneGenerator?.release()
    }
}
