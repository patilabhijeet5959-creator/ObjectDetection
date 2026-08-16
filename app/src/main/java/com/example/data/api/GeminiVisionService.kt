package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.DetectedItem
import com.example.data.model.NormalizedRect
import com.example.data.model.ObjectCategory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class InlineDataPart(
    @Json(name = "mime_type") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class TextPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class ApiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inline_data") val inlineData: InlineDataPart? = null
)

@JsonClass(generateAdapter = true)
data class ApiContent(
    @Json(name = "parts") val parts: List<ApiPart>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float = 0.2f,
    @Json(name = "response_mime_type") val responseMimeType: String = "application/json"
)

@JsonClass(generateAdapter = true)
data class GeminiVisionRequest(
    @Json(name = "contents") val contents: List<ApiContent>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig = GenerationConfig()
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: ApiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiVisionResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun analyzeImageWithFlashImage(
        @Query("key") apiKey: String,
        @Body request: GeminiVisionRequest
    ): GeminiVisionResponse

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeImageWithFlash(
        @Query("key") apiKey: String,
        @Body request: GeminiVisionRequest
    ): GeminiVisionResponse
}

class GeminiVisionClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if very large to ensure fast sub-second analysis
        val maxDim = 640
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (w, h) = if (ratio > 1f) {
                maxDim to (maxDim / ratio).toInt()
            } else {
                (maxDim * ratio).toInt() to maxDim
            }
            Bitmap.createScaledBitmap(bitmap, w, h, true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun detectObjects(
        bitmap: Bitmap,
        focusPoint: Pair<Float, Float>? = null
    ): Result<List<DetectedItem>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not set"))
        }

        val base64Data = bitmapToBase64(bitmap)

        val prompt = if (focusPoint != null) {
            """
            Analyze the object at normalized coordinates (x=${(focusPoint.first * 100).toInt()}%, y=${(focusPoint.second * 100).toInt()}%) and any surrounding objects in the image.
            Detect all distinct visible objects.
            Return a JSON array of objects strictly matching this format:
            [
              {
                "label": "Short Object Name",
                "category": "ELECTRONICS" | "PERSON" | "FURNITURE" | "VEHICLE" | "FOOD" | "ANIMAL" | "PLANT" | "CONTAINER" | "DOCUMENT" | "CLOTHING" | "OTHER",
                "confidence": 0.95,
                "box_2d": [ymin, xmin, ymax, xmax],
                "description": "Brief 1-sentence detail or color/pose"
              }
            ]
            Note: box_2d coordinates must be integers normalized between 0 and 1000 where [0,0,1000,1000] is the whole image.
            """.trimIndent()
        } else {
            """
            Detect and identify all prominent objects visible in this camera frame.
            For each object, provide its bounding box and category.
            Return a JSON array strictly in this format:
            [
              {
                "label": "Short Object Name",
                "category": "ELECTRONICS" | "PERSON" | "FURNITURE" | "VEHICLE" | "FOOD" | "ANIMAL" | "PLANT" | "CONTAINER" | "DOCUMENT" | "CLOTHING" | "OTHER",
                "confidence": 0.92,
                "box_2d": [ymin, xmin, ymax, xmax],
                "description": "Brief 1-sentence detail or color/pose"
              }
            ]
            Note: box_2d coordinates must be integers from 0 to 1000 (representing [top, left, bottom, right]).
            """.trimIndent()
        }

        val request = GeminiVisionRequest(
            contents = listOf(
                ApiContent(
                    parts = listOf(
                        ApiPart(text = prompt),
                        ApiPart(inlineData = InlineDataPart("image/jpeg", base64Data))
                    )
                )
            )
        )

        try {
            val response = try {
                api.analyzeImageWithFlashImage(apiKey, request)
            } catch (e: Exception) {
                // Fallback to flash
                api.analyzeImageWithFlash(apiKey, request)
            }

            val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("Empty response from AI"))

            val items = parseDetectionsJson(textResponse)
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeepObjectInsights(
        bitmap: Bitmap,
        item: DetectedItem,
        userQuestion: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not set"))
        }

        val base64Data = bitmapToBase64(bitmap)
        val prompt = if (!userQuestion.isNullOrBlank()) {
            "Regarding the ${item.label} located in the frame: $userQuestion. Provide a clear, concise, informative answer in 2-3 sentences."
        } else {
            "Provide deep analysis for the detected ${item.label} (${item.category.displayName}). Include key features, estimated material/specs, condition, and useful insights. Keep it concise (3 bullet points)."
        }

        val request = GeminiVisionRequest(
            contents = listOf(
                ApiContent(
                    parts = listOf(
                        ApiPart(text = prompt),
                        ApiPart(inlineData = InlineDataPart("image/jpeg", base64Data))
                    )
                )
            ),
            generationConfig = GenerationConfig(responseMimeType = "text/plain")
        )

        try {
            val response = api.analyzeImageWithFlash(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No analysis available"
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDetectionsJson(jsonStr: String): List<DetectedItem> {
        val detected = mutableListOf<DetectedItem>()
        try {
            // Clean up possible markdown code fences
            var cleanJson = jsonStr.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.removePrefix("```json")
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.removePrefix("```")
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.removeSuffix("```")
            }
            cleanJson = cleanJson.trim()

            val jsonArray = if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else if (cleanJson.startsWith("{")) {
                val obj = JSONObject(cleanJson)
                when {
                    obj.has("objects") -> obj.getJSONArray("objects")
                    obj.has("detections") -> obj.getJSONArray("detections")
                    obj.has("items") -> obj.getJSONArray("items")
                    else -> JSONArray()
                }
            } else {
                JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val label = obj.optString("label", "Object")
                val categoryStr = obj.optString("category", "")
                val category = ObjectCategory.fromString(categoryStr)
                val confidence = obj.optDouble("confidence", 0.85).toFloat().coerceIn(0.1f, 1.0f)
                val desc = obj.optString("description", "")

                val boxArray = obj.optJSONArray("box_2d")
                val box = if (boxArray != null && boxArray.length() == 4) {
                    val ymin = boxArray.optDouble(0, 0.0)
                    val xmin = boxArray.optDouble(1, 0.0)
                    val ymax = boxArray.optDouble(2, 1000.0)
                    val xmax = boxArray.optDouble(3, 1000.0)
                    // If values > 1.0, they are in 0..1000 range
                    val factor = if (ymin > 1.0 || xmin > 1.0 || ymax > 1.0 || xmax > 1.0) 1000f else 1f
                    NormalizedRect(
                        top = (ymin / factor).toFloat(),
                        left = (xmin / factor).toFloat(),
                        bottom = (ymax / factor).toFloat(),
                        right = (xmax / factor).toFloat()
                    ).clamped()
                } else {
                    NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f)
                }

                detected.add(
                    DetectedItem(
                        label = label,
                        confidence = confidence,
                        box = box,
                        category = category,
                        description = desc
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return detected
    }
}
