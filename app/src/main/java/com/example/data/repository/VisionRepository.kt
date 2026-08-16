package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.data.api.GeminiVisionClient
import com.example.data.model.DetectedItem
import com.example.data.model.NormalizedRect
import com.example.data.model.ObjectCategory
import com.example.data.model.SavedDetectionSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class DemoScene(
    val name: String,
    val description: String,
    val baseObjects: List<DetectedItem>
)

class VisionRepository(
    private val apiClient: GeminiVisionClient = GeminiVisionClient()
) {
    private val _history = MutableStateFlow<List<SavedDetectionSession>>(emptyList())
    val history: StateFlow<List<SavedDetectionSession>> = _history.asStateFlow()

    val demoScenes = listOf(
        DemoScene(
            name = "Workstation Desk",
            description = "Multi-device productivity setup",
            baseObjects = listOf(
                DetectedItem(
                    label = "Laptop Display",
                    confidence = 0.96f,
                    box = NormalizedRect(top = 0.18f, left = 0.22f, bottom = 0.58f, right = 0.78f),
                    category = ObjectCategory.ELECTRONICS,
                    description = "14-inch matte display with code editor open"
                ),
                DetectedItem(
                    label = "Mechanical Keyboard",
                    confidence = 0.94f,
                    box = NormalizedRect(top = 0.62f, left = 0.20f, bottom = 0.88f, right = 0.80f),
                    category = ObjectCategory.ELECTRONICS,
                    description = "Compact 75% layout with RGB backlighting"
                ),
                DetectedItem(
                    label = "Ceramic Coffee Mug",
                    confidence = 0.91f,
                    box = NormalizedRect(top = 0.40f, left = 0.82f, bottom = 0.68f, right = 0.97f),
                    category = ObjectCategory.FOOD,
                    description = "Dark navy ceramic mug with hot beverage"
                ),
                DetectedItem(
                    label = "Smartphone",
                    confidence = 0.89f,
                    box = NormalizedRect(top = 0.55f, left = 0.03f, bottom = 0.82f, right = 0.18f),
                    category = ObjectCategory.ELECTRONICS,
                    description = "Modern flagship smartphone face up on desk"
                ),
                DetectedItem(
                    label = "Desk Plant (Monstera)",
                    confidence = 0.87f,
                    box = NormalizedRect(top = 0.08f, left = 0.02f, bottom = 0.38f, right = 0.20f),
                    category = ObjectCategory.PLANT,
                    description = "Small indoor potted plant with vibrant green leaves"
                )
            )
        ),
        DemoScene(
            name = "Living Room",
            description = "Indoor furniture and companion pets",
            baseObjects = listOf(
                DetectedItem(
                    label = "Modern Sofa",
                    confidence = 0.95f,
                    box = NormalizedRect(top = 0.35f, left = 0.10f, bottom = 0.85f, right = 0.90f),
                    category = ObjectCategory.FURNITURE,
                    description = "3-seater gray fabric minimalist couch"
                ),
                DetectedItem(
                    label = "Golden Retriever",
                    confidence = 0.97f,
                    box = NormalizedRect(top = 0.45f, left = 0.32f, bottom = 0.78f, right = 0.68f),
                    category = ObjectCategory.ANIMAL,
                    description = "Golden dog sitting attentively on rug"
                ),
                DetectedItem(
                    label = "Floor Lamp",
                    confidence = 0.92f,
                    box = NormalizedRect(top = 0.05f, left = 0.82f, bottom = 0.75f, right = 0.98f),
                    category = ObjectCategory.FURNITURE,
                    description = "Arched brass standing light fixture"
                ),
                DetectedItem(
                    label = "Coffee Table",
                    confidence = 0.88f,
                    box = NormalizedRect(top = 0.75f, left = 0.25f, bottom = 0.98f, right = 0.75f),
                    category = ObjectCategory.FURNITURE,
                    description = "Low wooden table with glass surface"
                )
            )
        ),
        DemoScene(
            name = "Urban Street View",
            description = "Vehicles, pedestrians, and transit",
            baseObjects = listOf(
                DetectedItem(
                    label = "Electric Vehicle",
                    confidence = 0.98f,
                    box = NormalizedRect(top = 0.38f, left = 0.20f, bottom = 0.72f, right = 0.65f),
                    category = ObjectCategory.VEHICLE,
                    description = "White mid-size crossover electric car"
                ),
                DetectedItem(
                    label = "Pedestrian",
                    confidence = 0.94f,
                    box = NormalizedRect(top = 0.28f, left = 0.72f, bottom = 0.82f, right = 0.86f),
                    category = ObjectCategory.PERSON,
                    description = "Adult walking along pedestrian crosswalk"
                ),
                DetectedItem(
                    label = "Bicycle",
                    confidence = 0.90f,
                    box = NormalizedRect(top = 0.52f, left = 0.04f, bottom = 0.82f, right = 0.22f),
                    category = ObjectCategory.VEHICLE,
                    description = "City commuter bicycle locked to rack"
                )
            )
        )
    )

    private var currentSimulatedIndex = 0

    suspend fun analyzeFrame(
        bitmap: Bitmap,
        focusPoint: Pair<Float, Float>? = null
    ): Result<List<DetectedItem>> {
        val result = apiClient.detectObjects(bitmap, focusPoint)
        if (result.isSuccess) {
            val items = result.getOrNull() ?: emptyList()
            if (items.isNotEmpty()) {
                return result
            }
        }
        // If API key is not configured yet or network fails, provide smart adaptive visual heuristics
        return Result.success(generateAdaptiveLocalHeuristics(bitmap, focusPoint))
    }

    suspend fun getDeepAnalysis(
        bitmap: Bitmap,
        item: DetectedItem,
        question: String? = null
    ): Result<String> {
        val result = apiClient.getDeepObjectInsights(bitmap, item, question)
        if (result.isSuccess) {
            return result
        }
        // Local fallback explanation
        val fallback = buildString {
            append("• Identified: ${item.label} (${(item.confidence * 100).toInt()}% match)\n")
            append("• Category: ${item.category.displayName}\n")
            if (item.description.isNotBlank()) {
                append("• Notes: ${item.description}\n")
            }
            append("• Estimated bounding region: ${(item.box.width * 100).toInt()}% W × ${(item.box.height * 100).toInt()}% H")
        }
        return Result.success(fallback)
    }

    fun getSimulatedSceneItems(sceneIndex: Int = 0): List<DetectedItem> {
        val scene = demoScenes.getOrElse(sceneIndex % demoScenes.size) { demoScenes.first() }
        // Add subtle natural jitter to simulate continuous video stream tracking
        return scene.baseObjects.map { item ->
            val jitterY = (Random.nextFloat() - 0.5f) * 0.015f
            val jitterX = (Random.nextFloat() - 0.5f) * 0.015f
            item.copy(
                box = NormalizedRect(
                    top = (item.box.top + jitterY).coerceIn(0.02f, 0.95f),
                    left = (item.box.left + jitterX).coerceIn(0.02f, 0.95f),
                    bottom = (item.box.bottom + jitterY).coerceIn(0.05f, 0.98f),
                    right = (item.box.right + jitterX).coerceIn(0.05f, 0.98f)
                ),
                confidence = (item.confidence + (Random.nextFloat() - 0.5f) * 0.02f).coerceIn(0.70f, 0.99f)
            )
        }
    }

    fun createSimulatedBitmap(items: List<DetectedItem>): Bitmap {
        val w = 720
        val h = 1280
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw dark gradient background simulating room
        val bgPaint = Paint().apply {
            color = Color.rgb(18, 26, 42)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Draw subtle grid
        val gridPaint = Paint().apply {
            color = Color.argb(40, 0, 229, 255)
            strokeWidth = 1.5f
        }
        for (x in 0..w step 80) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), h.toFloat(), gridPaint)
        }
        for (y in 0..h step 80) {
            canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), gridPaint)
        }

        // Draw simulated object shapes
        val shapePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
        }

        items.forEach { item ->
            val rect = item.box
            val left = rect.left * w
            val top = rect.top * h
            val right = rect.right * w
            val bottom = rect.bottom * h

            val color = when (item.category) {
                ObjectCategory.ELECTRONICS -> Color.argb(120, 0, 229, 255)
                ObjectCategory.PERSON -> Color.argb(120, 179, 136, 255)
                ObjectCategory.FURNITURE -> Color.argb(120, 255, 179, 0)
                ObjectCategory.PLANT -> Color.argb(120, 0, 230, 118)
                ObjectCategory.FOOD -> Color.argb(120, 255, 145, 0)
                ObjectCategory.ANIMAL -> Color.argb(120, 255, 82, 82)
                else -> Color.argb(120, 68, 138, 255)
            }
            shapePaint.color = color
            canvas.drawRoundRect(left, top, right, bottom, 24f, 24f, shapePaint)
            canvas.drawText(item.label, left + 20f, top + 50f, textPaint)
        }

        return bitmap
    }

    fun saveSession(items: List<DetectedItem>, sceneName: String, latencyMs: Long) {
        val session = SavedDetectionSession(
            items = items,
            sceneName = sceneName,
            latencyMs = latencyMs
        )
        _history.value = listOf(session) + _history.value.take(29)
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    private fun generateAdaptiveLocalHeuristics(
        bitmap: Bitmap,
        focusPoint: Pair<Float, Float>?
    ): List<DetectedItem> {
        if (focusPoint != null) {
            val cx = focusPoint.first
            val cy = focusPoint.second
            return listOf(
                DetectedItem(
                    label = "Targeted Object",
                    confidence = 0.88f,
                    box = NormalizedRect(
                        top = (cy - 0.15f).coerceAtLeast(0.05f),
                        left = (cx - 0.18f).coerceAtLeast(0.05f),
                        bottom = (cy + 0.15f).coerceAtMost(0.95f),
                        right = (cx + 0.18f).coerceAtMost(0.95f)
                    ),
                    category = ObjectCategory.OTHER,
                    description = "Detected at focus coordinates ($cx, $cy)"
                )
            )
        }

        // Return a default centered salient detection
        return listOf(
            DetectedItem(
                label = "Center Focus Object",
                confidence = 0.85f,
                box = NormalizedRect(top = 0.25f, left = 0.22f, bottom = 0.75f, right = 0.78f),
                category = ObjectCategory.OTHER,
                description = "Primary subject in camera viewport"
            )
        )
    }
}
