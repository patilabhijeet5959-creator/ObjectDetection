package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberViolet
import java.util.UUID

enum class ObjectCategory(val displayName: String) {
    ELECTRONICS("Electronics"),
    PERSON("Person"),
    FURNITURE("Furniture"),
    VEHICLE("Vehicle"),
    FOOD("Food & Beverage"),
    ANIMAL("Animal"),
    PLANT("Plant / Nature"),
    CONTAINER("Container / Tool"),
    DOCUMENT("Document / Text"),
    CLOTHING("Apparel"),
    OTHER("Object");

    fun getColor(): Color = when (this) {
        ELECTRONICS -> CyberCyan
        PERSON -> CyberViolet
        FURNITURE -> CyberAmber
        VEHICLE -> CyberBlue
        FOOD -> CyberOrange
        ANIMAL -> CyberRose
        PLANT -> CyberEmerald
        CONTAINER -> CyberCyan
        DOCUMENT -> CyberAmber
        CLOTHING -> CyberViolet
        OTHER -> CyberEmerald
    }

    fun getIcon(): ImageVector = when (this) {
        ELECTRONICS -> Icons.Default.Computer
        PERSON -> Icons.Default.Person
        FURNITURE -> Icons.Default.Weekend
        VEHICLE -> Icons.Default.DirectionsCar
        FOOD -> Icons.Default.LunchDining
        ANIMAL -> Icons.Default.Pets
        PLANT -> Icons.Default.LocalFlorist
        CONTAINER -> Icons.Default.LocalDrink
        DOCUMENT -> Icons.Default.Description
        CLOTHING -> Icons.Default.Person
        OTHER -> Icons.Default.Category
    }

    companion object {
        fun fromString(name: String?): ObjectCategory {
            if (name.isNullOrBlank()) return OTHER
            val upper = name.uppercase()
            return when {
                upper.contains("PERSON") || upper.contains("HUMAN") || upper.contains("FACE") -> PERSON
                upper.contains("PHONE") || upper.contains("LAPTOP") || upper.contains("COMPUTER") ||
                        upper.contains("SCREEN") || upper.contains("TECH") || upper.contains("ELECTRONIC") ||
                        upper.contains("KEYBOARD") || upper.contains("MOUSE") || upper.contains("HEADPHONE") -> ELECTRONICS
                upper.contains("CHAIR") || upper.contains("TABLE") || upper.contains("DESK") ||
                        upper.contains("SOFA") || upper.contains("BED") || upper.contains("FURNITURE") -> FURNITURE
                upper.contains("CAR") || upper.contains("BIKE") || upper.contains("VEHICLE") ||
                        upper.contains("BUS") || upper.contains("TRUCK") -> VEHICLE
                upper.contains("FOOD") || upper.contains("FRUIT") || upper.contains("DRINK") ||
                        upper.contains("BOTTLE") || upper.contains("CUP") || upper.contains("MUG") ||
                        upper.contains("COFFEE") || upper.contains("MEAL") -> FOOD
                upper.contains("CAT") || upper.contains("DOG") || upper.contains("BIRD") ||
                        upper.contains("ANIMAL") || upper.contains("PET") -> ANIMAL
                upper.contains("PLANT") || upper.contains("FLOWER") || upper.contains("TREE") ||
                        upper.contains("LEAF") || upper.contains("BOTANY") -> PLANT
                upper.contains("BOOK") || upper.contains("PAPER") || upper.contains("DOCUMENT") ||
                        upper.contains("TEXT") -> DOCUMENT
                upper.contains("SHIRT") || upper.contains("JACKET") || upper.contains("HAT") ||
                        upper.contains("CLOTH") -> CLOTHING
                else -> values().find { it.name.equals(upper, ignoreCase = true) } ?: OTHER
            }
        }
    }
}

/**
 * Normalized 2D Bounding Box in coordinates from 0.0 to 1.0
 * (0,0) is top-left, (1,1) is bottom-right.
 */
data class NormalizedRect(
    val top: Float,    // ymin
    val left: Float,   // xmin
    val bottom: Float, // ymax
    val right: Float   // xmax
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f

    fun clamped(): NormalizedRect = NormalizedRect(
        top = top.coerceIn(0f, 1f),
        left = left.coerceIn(0f, 1f),
        bottom = bottom.coerceIn(0f, 1f),
        right = right.coerceIn(0f, 1f)
    )
}

data class DetectedItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val confidence: Float,
    val box: NormalizedRect,
    val category: ObjectCategory = ObjectCategory.OTHER,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class DetectionMode {
    CONTINUOUS, // Automatically scans live feed every interval
    BURST,      // Deep inspection on user demand / capture
    TAP_TARGET  // User taps on preview to inspect specific region
}

data class VisionSettings(
    val scanIntervalMs: Long = 1800L,
    val confidenceThreshold: Float = 0.35f,
    val showLabels: Boolean = true,
    val showConfidence: Boolean = true,
    val enableLaserScanEffect: Boolean = true,
    val enableAudioCues: Boolean = false,
    val categoryFilter: ObjectCategory? = null,
    val detectionMode: DetectionMode = DetectionMode.CONTINUOUS,
    val isTorchEnabled: Boolean = false,
    val isFrontCamera: Boolean = false,
    val isFrozen: Boolean = false,
    val isSimulationActive: Boolean = false
)

data class SavedDetectionSession(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<DetectedItem>,
    val sceneName: String = "Live Scan",
    val latencyMs: Long = 0L
)
