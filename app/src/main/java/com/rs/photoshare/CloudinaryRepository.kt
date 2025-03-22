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

            // Request body - Make sure to request both context and tags
            val body = JSONObject().apply {
                put("expression", expression)
                put("timestamp", timestamp)
                put("api_key", API_KEY)
                put("signature", signature)
                put("max_results", 500)
                // Make sure we explicitly request context and tags
                put("with_field", JSONArray().apply {
                    put("context")
                    put("tags")
                })
                // Return specific fields including context and tags
                put("return_fields", JSONArray().apply {
                    put("public_id")
                    put("secure_url")
                    put("context")
                    put("tags")
                })
            }

            Log.d(TAG, "Request body: ${body.toString()}")

            val outputBytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(outputBytes) }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response body: $response")
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

            // Debug log to see what we're getting from Cloudinary
            Log.d(TAG, "Raw resource data: ${resource.toString()}")

            // Extract context metadata - first check if context exists
            val contextObj = resource.optJSONObject("context")

            // Log the context structure for debugging
            if (contextObj != null) {
                Log.d(TAG, "Context structure: ${contextObj.toString()}")
            } else {
                Log.d(TAG, "No context found in resource")
            }

            // FIXED: Improved metadata extraction strategy
            // First initialize with direct title/description if available
            var title = "Untitled"
            var description = ""
            var tagsString = ""
            var creatorId = "unknown"
            var timestamp = System.currentTimeMillis()
            var likes = 0
            var dislikes = 0

            // Step 1: Try to get direct properties from context
            if (contextObj != null) {
                // Try to get title and description directly
                if (contextObj.has("title")) {
                    title = contextObj.getString("title")
                }
                if (contextObj.has("description")) {
                    description = contextObj.getString("description")
                }

                // Step 2: Try to parse the custom field if available
                if (contextObj.has("custom")) {
                    val customStr = contextObj.getString("custom")
                    Log.d(TAG, "Custom context: $customStr")

                    // Parse the custom string
                    customStr.split("|").forEach { pair ->
                        val keyValue = pair.split("=", limit = 2)
                        if (keyValue.size == 2) {
                            val key = keyValue[0].trim()
                            val value = keyValue[1].trim()

                            // Update metadata based on the key
                            when (key) {
                                "title" -> if (title == "Untitled") title = value
                                "description" -> if (description.isEmpty()) description = value
                                "tags" -> tagsString = value
                                "creator_id" -> creatorId = value
                                "timestamp" -> timestamp = value.toLongOrNull() ?: timestamp
                                "likes" -> likes = value.toIntOrNull() ?: likes
                                "dislikes" -> dislikes = value.toIntOrNull() ?: dislikes
                            }
                        }
                    }
                }
            }

            // Parse tags - handle resource tags separately
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

            val artPiece = ArtPiece(
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

            // Log the final parsed result
            Log.d(TAG, "Parsed ArtPiece: $artPiece")

            return artPiece
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing resource to ArtPiece: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Updates the metadata for an existing art piece in Cloudinary.
     * This is particularly useful for updating likes and dislikes.
     */
    suspend fun updateArtPieceMetadata(artPiece: ArtPiece): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis() / 1000

            // Create the context string with updated metadata
            val contextStr = "title=${artPiece.title}|description=${artPiece.description}|" +
                    "tags=${artPiece.tags.joinToString(",")}|" +
                    "creator_id=${artPiece.creatorId}|" +
                    "timestamp=${artPiece.timestamp}|" +
                    "likes=${artPiece.likes}|" +
                    "dislikes=${artPiece.dislikes}"

            // Create signature for authentication
            val toSign = "context=$contextStr&public_id=$FOLDER_PATH/${artPiece.artId}&timestamp=$timestamp$API_SECRET"
            val signature = generateSHA1Signature(toSign)

            // Build URL for updating context metadata
            val urlString = "https://api.cloudinary.com/v1_1/$CLOUDINARY_NAME/image/context" +
                    "?public_id=$FOLDER_PATH/${artPiece.artId}" +
                    "&context=$contextStr" +
                    "&timestamp=$timestamp" +
                    "&api_key=$API_KEY" +
                    "&signature=$signature"

            Log.d(TAG, "Updating metadata at: $urlString")

            // Create connection
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Get response
            val responseCode = connection.responseCode
            Log.d(TAG, "Update metadata response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "HTTP error updating metadata: $responseCode, Response: $errorResponse")
            }

            return@withContext responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e(TAG, "Error updating art piece metadata in Cloudinary", e)
            return@withContext false
        }
    }

    // In CloudinaryRepository.kt
    suspend fun updateLikesAndDislikes(artPiece: ArtPiece, isLiked: Boolean, userId: String): Boolean {
        val updatedArtPiece = if (isLiked) {
            // Add user to likedBy list if not already there
            if (!artPiece.likedBy.contains(userId)) {
                artPiece.copy(
                    likes = artPiece.likes + 1,
                    likedBy = artPiece.likedBy + userId
                )
            } else {
                artPiece
            }
        } else {
            // Add user to dislikedBy list if not already there
            if (!artPiece.dislikedBy.contains(userId)) {
                artPiece.copy(
                    dislikes = artPiece.dislikes + 1,
                    dislikedBy = artPiece.dislikedBy + userId
                )
            } else {
                artPiece
            }
        }

        // Update metadata in Cloudinary
        return updateArtPieceMetadata(updatedArtPiece)
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