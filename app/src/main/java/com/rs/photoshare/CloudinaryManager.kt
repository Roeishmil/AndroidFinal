package com.rs.photoshare

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import java.util.HashMap

class CloudinaryManager(private val context: Context) {

    companion object {
        private var isInitialized = false

        fun initialize(context: Context) {
            if (!isInitialized) {
                try {
                    val config = HashMap<String, String>()
                    config["cloud_name"] = "dsgm464ox"
                    config["api_key"] = "836655194811561"
                    config["api_secret"] = "uszkm7JffuAgMVGP4jV33cTD4So"
                    config["secure"] = "true"

                    MediaManager.init(context, config)
                    isInitialized = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Cloudinary initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun uploadImage(imageFile: File, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (!isInitialized) {
            initialize(context)
        }

        val requestId = MediaManager.get().upload(imageFile.absolutePath)
            .unsigned("ml_default")
            .option("folder", "art_pieces")
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Upload started
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Track upload progress if needed
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    if (imageUrl != null) {
                        onSuccess(imageUrl)
                    } else {
                        onError("Upload successful but URL not found")
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError("Upload failed: ${error.description}")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Rescheduled upload
                }
            })
            .dispatch()
    }

    fun uploadImageFromUri(uri: Uri, artId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            onError("Failed to read image data")
            return
        }

        // Create a temporary file
        val tempFile = File(context.cacheDir, "temp_upload_$artId.jpg")
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()

        // Upload the temp file
        uploadImage(tempFile, onSuccess) { error ->
            tempFile.delete()
            onError(error)
        }
    }
}