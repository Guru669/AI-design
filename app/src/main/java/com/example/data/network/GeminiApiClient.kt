package com.example.data.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini Content Models for Request/Response ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded string
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

/**
 * Custom direct HTTP/OKHttp client for Gemini API.
 * Uses Moshi for serialization to ensure compatibility and simplicity.
 */
object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GenerateContentRequest::class.java)
    private val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the Gemini API Key is configured.
     */
    fun isApiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder")
    }

    /**
     * Helper to compress bitmap and convert to Base64
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to keep within payload limits
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Performs a multimodal generation request (Base64 Image + Text prompt).
     */
    suspend fun analyzeImage(bitmap: Bitmap, prompt: String, systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext "API_KEY_MISSING_FALLBACK_DEMO"
        }

        try {
            val base64Image = bitmap.toBase64()
            val requestBodyObj = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                systemInstruction = systemPrompt?.let {
                    Content(parts = listOf(Part(text = it)))
                }
            )

            val jsonBody = requestAdapter.toJson(requestBodyObj)
            val httpUrl = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"

            val httpRequest = Request.Builder()
                .url(httpUrl)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API call failed with code ${response.code}: $errorMsg")
                    return@withContext "Error ${response.code}: Failed to communicate with AI Services."
                }

                val responseBodyStr = response.body?.string()
                if (responseBodyStr.isNullOrEmpty()) {
                    return@withContext "Empty response received from AI model."
                }

                val responseObj = responseAdapter.fromJson(responseBodyStr)
                responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Could not gather design suggestions. Check the space parameters."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during image analysis", e)
            "Error matching your room design. Please check your connection: ${e.localizedMessage}"
        }
    }

    /**
     * Performs standard text-based assistant queries / conversation.
     */
    suspend fun chat(prompt: String, history: List<Content> = emptyList(), systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            return@withContext "API_KEY_MISSING_FALLBACK_DEMO"
        }

        try {
            // Re-map history to Content structure
            val contentsList = mutableListOf<Content>()
            contentsList.addAll(history)
            contentsList.add(Content(role = "user", parts = listOf(Part(text = prompt))))

            val requestBodyObj = GenerateContentRequest(
                contents = contentsList,
                systemInstruction = systemPrompt?.let {
                    Content(parts = listOf(Part(text = it)))
                }
            )

            val jsonBody = requestAdapter.toJson(requestBodyObj)
            val httpUrl = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"

            val httpRequest = Request.Builder()
                .url(httpUrl)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini Chat call failed with code ${response.code}: $errorMsg")
                    return@withContext "Error: Applet was unable to retrieve AI response."
                }

                val responseBodyStr = response.body?.string()
                if (responseBodyStr.isNullOrEmpty()) {
                    return@withContext "Model responded with empty suggestion."
                }

                val responseObj = responseAdapter.fromJson(responseBodyStr)
                responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "The design assistant is currently recharging. Try again."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during chat", e)
            "Error: ${e.localizedMessage}"
        }
    }
}
