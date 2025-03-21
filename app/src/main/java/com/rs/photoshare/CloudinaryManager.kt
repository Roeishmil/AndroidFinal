package com.rs.photoshare

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.rs.photoshare.models.ArtPiece
import java.io.File
import java.util.HashMap

/**
 * Manages Cloudinary integration for uploading images.
 * This class handles initialization, file uploads, and secure storage of images.
 */
class CloudinaryManager(private val context: Context) {

    companion object {
        private var isInitialized = false

        // Moving credentials to a separate method to improve security and organization
        private fun getCloudinaryConfig(): HashMap<String, String> {
            return HashMap<String, String>().apply {
                put("cloud_name", "dsgm464ox")
                put("api_key", "836655194811561")
                put("api_secret", "uszkm7JffuAgMVGP4jV33cTD4So")
                put("secure", "true")
            }
        }

        /**
         * Initializes the Cloudinary MediaManager with the provided configuration.
         * Ensures initialization only happens once to prevent redundant setups.
         *
         * @param context The application context.
         */
        fun initialize(context: Context) {
            if (!isInitialized) {
                try {
                    val config = getCloudinaryConfig()
                    MediaManager.init(context, config)
                    isInitialized = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Cloudinary initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Uploads an image file to Cloudinary.
     *
     * @param imageFile The file to be uploaded.
     * @param artPiece The ArtPiece object containing metadata to be stored with the image.
     * @param onSuccess A callback function invoked with the uploaded image URL if successful.
     * @param onError A callback function invoked with an error message if the upload fails.
     */
    fun uploadImage(imageFile: File, artPiece: ArtPiece, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (!isInitialized) {
            initialize(context)
        }

        // Create context metadata to store with the image
        val metadata = mapOf(
            "title" to artPiece.title,
            "description" to artPiece.description,
            "tags" to artPiece.tags.joinToString(","),
            "creator_id" to artPiece.creatorId,
            "timestamp" to artPiece.timestamp.toString(),
            "likes" to artPiece.likes.toString(),
            "dislikes" to artPiece.dislikes.toString()
        )

        // Convert metadata to the format Cloudinary expects
        val context = metadata.entries.joinToString("|") { "${it.key}=${it.value}" }

        MediaManager.get().upload(imageFile.absolutePath)
            .unsigned("ml_default") // Uses an unsigned preset for security
            .option("folder", "art_pieces") // Stores images inside the "art_pieces" folder
            .option("resource_type", "image")
            .option("public_id", artPiece.artId) // Use artId as public_id for better tracking
            .option("context", "custom=$context") // Add metadata as context
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Triggered when upload starts
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Can be used to track upload progress
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    if (imageUrl != null) {
                        onSuccess(imageUrl) // Pass the image URL to the success callback
                    } else {
                        onError("Upload successful but URL not found")
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError("Upload failed: ${error.description}")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Triggered when the upload is rescheduled due to network issues
                }
            })
            .dispatch()
    }

    /**
     * Uploads an image from a URI. This method first saves the image as a temporary file before uploading.
     *
     * @param uri The URI of the image to be uploaded.
     * @param artPiece The ArtPiece object containing metadata to be stored with the image.
     * @param onSuccess A callback function invoked with the uploaded image URL if successful.
     * @param onError A callback function invoked with an error message if the upload fails.
     */
    fun uploadImageFromUri(uri: Uri, artPiece: ArtPiece, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            onError("Failed to read image data")
            return
        }

        // Create a temporary file in the cache directory
        val tempFile = File(context.cacheDir, "temp_upload_${artPiece.artId}.jpg")

        try {
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream) // Copy data from the URI stream to the temp file
            }
            inputStream.close()

            // Upload the temporary file with metadata
            uploadImage(tempFile, artPiece, onSuccess) { error ->
                tempFile.delete() // Delete the temporary file in case of failure
                onError(error)
            }
        } catch (e: Exception) {
            onError("Failed to create temporary file: ${e.message}")
        }
    }
}