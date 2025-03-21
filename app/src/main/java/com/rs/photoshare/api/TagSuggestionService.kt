package com.rs.photoshare.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service to suggest tags using OpenAI's API, with caching and rate limiting.
 */
class TagSuggestionService(private val context: Context) {
    private val apiKey = "" // TODO: Move to BuildConfig or secure storage.
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("tag_suggestions", Context.MODE_PRIVATE)

    // Rate limiting: 10 requests per minute.
    private val requestWindowMs = 60000 // 1 minute
    private val maxRequestsPerWindow = 10
    private var requestTimestamps = mutableListOf<Long>()

    // Cache expiry set to 24 hours.
    private val cacheExpiryMs = 24 * 60 * 60 * 1000 // 24 hours

    init {
        // Load and clean up previous request timestamps.
        val savedTimestamps = prefs.getString("request_timestamps", null)
        if (savedTimestamps != null) {
            val type = object : TypeToken<List<Long>>() {}.type
            requestTimestamps = gson.fromJson(savedTimestamps, type)
            val now = System.currentTimeMillis()
            requestTimestamps = requestTimestamps.filter { now - it < requestWindowMs }.toMutableList()
            saveRequestTimestamps()
        }
    }

    /**
     * Suggest tags based on user input and a list of existing tags.
     */
    suspend fun suggestTags(userInput: String, existingTags: List<String>): List<String> {
        // Check for cached suggestions.
        val cacheKey = generateCacheKey(userInput, existingTags)
        val cachedResult = getCachedSuggestions(cacheKey)
        if (cachedResult != null) {
            Log.d(TAG, "Using cached suggestions for: $userInput")
            return cachedResult
        }

        // Enforce rate limiting.
        if (!checkRateLimit()) {
            Log.w(TAG, "Rate limit exceeded for tag suggestions")
            return emptyList()
        }

        // Make the API request.
        return withContext(Dispatchers.IO) {
            try {
                val suggestions = makeApiRequest(userInput, existingTags)
                if (suggestions.isNotEmpty()) {
                    cacheSuggestions(cacheKey, suggestions)
                }
                suggestions
            } catch (e: Exception) {
                Log.e(TAG, "Error getting tag suggestions", e)
                emptyList()
            }
        }
    }

    /**
     * Makes an API call to OpenAI for tag suggestions.
     */
    private fun makeApiRequest(userInput: String, existingTags: List<String>): List<String> {
        val json = gson.toJson(ChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are a helpful assistant that suggests one-word tags for art posts. " +
                            "Return ONLY a one-word tag without any additional text. " +
                            "Focus on character attributes, colors, styles, emotions, and themes."
                ),
                ChatMessage(
                    role = "user",
                    content = "Suggest tags for this art post: $userInput\n\n" +
                            "Popular tags: ${existingTags.joinToString(", ")}"
                )
            ),
            max_tokens = 100,
            temperature = 0.7
        ))

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response: ${response.code}")
            }

            val responseBody = response.body?.string() ?: ""
            val completionResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)

            val tagsText = completionResponse.choices.firstOrNull()?.message?.content ?: ""
            Log.d(TAG, "Parsed tag text: $tagsText")
            return tagsText.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    /**
     * Generates a cache key from user input and existing tags.
     */
    private fun generateCacheKey(userInput: String, existingTags: List<String>): String {
        val normalizedInput = userInput.trim().lowercase()
        val normalizedTags = existingTags.map { it.trim().lowercase() }.sorted().joinToString(",")
        return "$normalizedInput|$normalizedTags"
    }

    /**
     * Retrieves suggestions from cache if available and not expired.
     */
    private fun getCachedSuggestions(cacheKey: String): List<String>? {
        val cachedData = prefs.getString("cache_$cacheKey", null) ?: return null
        val timestamp = prefs.getLong("cache_time_$cacheKey", 0)
        if (System.currentTimeMillis() - timestamp > cacheExpiryMs) {
            return null
        }
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(cachedData, type)
    }

    /**
     * Caches suggestions along with the current timestamp.
     */
    private fun cacheSuggestions(cacheKey: String, suggestions: List<String>) {
        prefs.edit()
            .putString("cache_$cacheKey", gson.toJson(suggestions))
            .putLong("cache_time_$cacheKey", System.currentTimeMillis())
            .apply()
    }

    /**
     * Checks if the rate limit has been reached.
     */
    @Synchronized
    private fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        // Remove old timestamps
        requestTimestamps = requestTimestamps.filter { now - it < requestWindowMs }.toMutableList()

        // Check if we've hit the limit
        if (requestTimestamps.size >= maxRequestsPerWindow) {
            return false
        }

        // Add the current timestamp BEFORE making the request
        requestTimestamps.add(now)
        saveRequestTimestamps()
        return true
    }

    /**
     * Persists request timestamps to shared preferences.
     */
    private fun saveRequestTimestamps() {
        prefs.edit()
            .putString("request_timestamps", gson.toJson(requestTimestamps))
            .apply()
    }

    companion object {
        private const val TAG = "TagSuggestionService"
    }
}

// Data classes for OpenAI API communication.
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int = 100,
    val temperature: Double = 0.7
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class Message(
    val content: String
)
