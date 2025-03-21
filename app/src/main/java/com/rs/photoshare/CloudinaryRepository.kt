package com.rs.photoshare

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.rs.photoshare.models.ArtPiece
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for interacting with Cloudinary to fetch art pieces.
 */
class CloudinaryRepository(private val context: Context) {

    companion object {
        private const val TAG = "CloudinaryRepository"

        // Hardcoded credentials
        private const val CLOUDINARY_NAME = "dsgm464ox"
        private const val API_KEY = "836655194811561"
        private const val API_SECRET = "uszkm7JffuAgMVGP4jV33cTD4So"

        // Base URL for Cloudinary's Admin & Search API
        private val CLOUDINARY_API_BASE = "https://$API_KEY:$API_SECRET@$CLOUDINARY_NAME"

        // Folder where art pieces are stored
        private const val FOLDER_PATH = "art_pieces"
    }

    /**
     * Fetches all art pieces from Cloudinary.
     * @return List of ArtPiece objects
     */
    suspend fun fetchAllArtPiecesAsync(): List<ArtPiece> = withContext(Dispatchers.IO) {
        try {
            // Get all resources from the art_pieces folder
            val resources = getCloudinaryResources()
            Log.d(TAG, "Found ${resources.length()} resources in Cloudinary")

            // Parse resources into ArtPiece objects
            val artPieces = mutableListOf<ArtPiece>()
            for (i in 0 until resources.length()) {
                val resource = resources.getJSONObject(i)
                val artPiece = parseResourceToArtPiece(resource)
                if (artPiece != null) {
                    artPieces.add(artPiece)
                }
            }

            Log.d(TAG, "Successfully parsed ${artPieces.size} art pieces from Cloudinary")
            return@withContext artPieces
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching art pieces from Cloudinary", e)
            throw e
        }
    }

    /**
     * Fetches all resources from the art_pieces folder in Cloudinary using the Search API.
     */
    private suspend fun getCloudinaryResources(): JSONArray = suspendCancellableCoroutine { continuation ->
        try {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val expression = "folder:$FOLDER_PATH"

            // Signature only includes expression + timestamp (alphabetically sorted)
            val toSign = "expression=$expression&timestamp=$timestamp$API_SECRET"
            val signature = generateSHA1Signature(toSign)

            // Cloudinary Search API endpoint
            val url = URL("https://api.cloudinary.com/v1_1/$CLOUDINARY_NAME/resources/search")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Basic authentication header (api_key:api_secret base64)
            val credentials = "$API_KEY:$API_SECRET"
            val basicAuth = "Basic " + android.util.Base64.encodeToString(credentials.toByteArray(), android.util.Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", basicAuth)

            // Request body
            val body = JSONObject().apply {
                put("expression", expression)
                put("timestamp", timestamp)
                put("api_key", API_KEY)
                put("signature", signature)
                put("max_results", 500)
                put("with_field", JSONArray().apply { put("context") }) // optional but safe
                put("return_fields", JSONArray().apply { put("context"); put("public_id"); put("secure_url"); put("tags") }) // Force it to return metadata
            }



            val outputBytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(outputBytes) }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("resources")) {
                    continuation.resume(jsonResponse.getJSONArray("resources"))
                } else {
                    Log.e(TAG, "Response doesn't contain resources: $response")
                    continuation.resume(JSONArray())
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "HTTP error: $responseCode, Response: $errorResponse")
                continuation.resumeWithException(Exception("HTTP error: $responseCode - $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Cloudinary resources", e)
            continuation.resumeWithException(e)
        }
    }


    /**
     * Generate a SHA-1 signature required for Cloudinary API authentication
     */
    private fun generateSHA1Signature(input: String): String {
        try {
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val digest = md.digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating SHA-1 signature", e)
            throw e
        }
    }

    /**
     * Parses a Cloudinary resource JSON object into an ArtPiece object.
     */
    private fun parseResourceToArtPiece(resource: JSONObject): ArtPiece? {
        try {
            val publicId = resource.getString("public_id").replace("$FOLDER_PATH/", "")
            val secureUrl = resource.getString("secure_url")

            // Extract context metadata if available
            val context = resource.optJSONObject("context")?.optJSONObject("custom") ?: JSONObject()

            // Extract metadata or use defaults
            val title = context.optString("title", "Untitled")
            val description = context.optString("description", "")
            val tagsString = context.optString("tags", "")
            val creatorId = context.optString("creator_id", "unknown")
            val timestamp = context.optLong("timestamp", System.currentTimeMillis())
            val likes = context.optInt("likes", 0)
            val dislikes = context.optInt("dislikes", 0)

            // Parse tags - handle both resource tags and context tags
            val tags = when {
                resource.has("tags") && resource.get("tags") is JSONArray -> {
                    val tagsArray = resource.getJSONArray("tags")
                    List(tagsArray.length()) { tagsArray.getString(it) }
                }
                tagsString.isNotEmpty() -> {
                    tagsString.split(",").map { it.trim() }
                }
                else -> {
                    listOf("Uncategorized")
                }
            }

            return ArtPiece(
                artId = publicId,
                title = title,
                description = description,
                imageUrl = secureUrl,
                tags = tags,
                creatorId = creatorId,
                timestamp = timestamp,
                likes = likes,
                dislikes = dislikes,
                likedBy = listOf(),  // We'll need to manage these locally
                dislikedBy = listOf() // We'll need to manage these locally
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing resource to ArtPiece: ${e.message}")
            return null
        }
    }

    /**
     * Deletes an art piece from Cloudinary.
     */
    suspend fun deleteArtPiece(artId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis() / 1000
            val toSign = "public_ids[]=$FOLDER_PATH/$artId&timestamp=$timestamp$API_SECRET"
            val signature = generateSHA1Signature(toSign)

            // Create admin API URL to delete the resource
            val urlString = "$CLOUDINARY_API_BASE/resources/image/destroy?public_ids[]=$FOLDER_PATH/$artId&api_key=$API_KEY&timestamp=$timestamp&signature=$signature"

            // Log connection details for debugging
            Log.d(TAG, "Deleting resource at: $urlString")

            // Create connection
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Get response
            val responseCode = connection.responseCode
            Log.d(TAG, "Delete response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "HTTP error deleting: $responseCode, Response: $errorResponse")
            }
            return@withContext responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting art piece from Cloudinary", e)
            return@withContext false
        }
    }
}