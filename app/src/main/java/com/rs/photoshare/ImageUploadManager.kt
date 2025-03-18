package com.rs.photoshare

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import com.google.firebase.auth.FirebaseAuth
import com.rs.photoshare.models.ArtPiece
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ImageUploadManager(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val progressBar: ProgressBar?,
    private val onArtPieceUploaded: (ArtPiece) -> Unit
) {

    private var imageUri: Uri? = null
    private val cloudinaryManager = CloudinaryManager(context)
    private val predefinedTags = listOf("Warrior", "Wizard", "Doctor", "Engineer", "Custom")

    fun startImagePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        launcher.launch(intent)
    }

    fun handleImageResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            showAddDetailsDialog()
        }
    }

    private fun showAddDetailsDialog() {
        val context = this.context
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val titleInput = EditText(context).apply { hint = "Enter title" }
        val descriptionInput = EditText(context).apply { hint = "Enter description" }

        // Create a multiple tag selection UI
        val tagLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val tagLabel = TextView(context).apply {
            text = "Select Tags (choose all that apply):"
            setPadding(0, 8, 0, 8)
        }

        tagLayout.addView(tagLabel)

        val selectedTags = mutableListOf<String>()
        val tagCheckboxes = mutableListOf<CheckBox>()

        // Add predefined tag checkboxes
        predefinedTags.forEach { tag ->
            val checkbox = CheckBox(context).apply {
                text = tag
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTags.add(tag)
                    } else {
                        selectedTags.remove(tag)
                    }
                }
            }
            tagCheckboxes.add(checkbox)
            tagLayout.addView(checkbox)
        }

        // Add custom tag input
        val customTagInput = EditText(context).apply {
            hint = "Enter custom tag (optional)"
            visibility = View.VISIBLE
        }

        layout.addView(titleInput)
        layout.addView(descriptionInput)
        layout.addView(tagLayout)
        layout.addView(customTagInput)

        AlertDialog.Builder(context)
            .setTitle("Add Art Piece Details")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                // Collect all selected tags
                val finalTags = selectedTags.toMutableList()

                // Add custom tag if provided
                val customTag = customTagInput.text.toString().trim()
                if (customTag.isNotEmpty()) {
                    finalTags.add(customTag)
                }

                // Remove "Custom" tag if it was selected but replace with actual custom tag
                if (finalTags.contains("Custom") && customTag.isNotEmpty()) {
                    finalTags.remove("Custom")
                }

                if (title.isEmpty() || description.isEmpty() || finalTags.isEmpty()) {
                    Toast.makeText(context, "Title, description and at least one tag are required", Toast.LENGTH_SHORT).show()
                } else {
                    uploadImageToCloudinary(title, description, finalTags)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadImageToCloudinary(title: String, description: String, tags: List<String>) {
        progressBar?.visibility = View.VISIBLE

        val imageUri = imageUri ?: return
        val artId = UUID.randomUUID().toString()

        cloudinaryManager.uploadImageFromUri(
            uri = imageUri,
            artId = artId,
            onSuccess = { cloudinaryUrl ->
                // Keep a local thumbnail for preview
                val thumbnailFile = createLocalThumbnail(imageUri, artId)

                val newArtPiece = ArtPiece(
                    artId = artId,
                    title = title,
                    description = description,
                    imageUrl = cloudinaryUrl,  // Store Cloudinary URL
                    tags = tags,
                    creatorId = auth.currentUser?.uid ?: "unknown",
                    timestamp = System.currentTimeMillis()
                )

                // Save metadata locally with Cloudinary URL
                saveArtPieceMetadata(newArtPiece, thumbnailFile?.absolutePath)
                onArtPieceUploaded(newArtPiece)
                progressBar?.visibility = View.GONE
            },
            onError = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                progressBar?.visibility = View.GONE
            }
        )
    }

    private fun createLocalThumbnail(uri: Uri, artId: String): File? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream)

                // Create a smaller thumbnail version
                val maxDimension = 300
                val thumbnailWidth: Int
                val thumbnailHeight: Int

                if (originalBitmap.width > originalBitmap.height) {
                    thumbnailWidth = maxDimension
                    thumbnailHeight = (maxDimension.toFloat() / originalBitmap.width * originalBitmap.height).toInt()
                } else {
                    thumbnailHeight = maxDimension
                    thumbnailWidth = (maxDimension.toFloat() / originalBitmap.height * originalBitmap.width).toInt()
                }

                val thumbnailBitmap = Bitmap.createScaledBitmap(originalBitmap, thumbnailWidth, thumbnailHeight, true)
                val thumbnailFile = File(context.filesDir, "thumb_${artId}.jpg")

                FileOutputStream(thumbnailFile).use { outputStream ->
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                }

                thumbnailFile
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to create thumbnail: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun saveArtPieceMetadata(artPiece: ArtPiece, thumbnailPath: String?) {
        val metadataFile = File(context.filesDir, "art_${artPiece.artId}.json")

        // This is a simple JSON string creation
        // You might want to use Gson or another JSON library in production
        val tagsJson = artPiece.tags.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"")

        val artPieceJson = """
        {
            "artId": "${artPiece.artId}",
            "title": "${artPiece.title}",
            "description": "${artPiece.description}",
            "imageUrl": "${artPiece.imageUrl}",
            "thumbnailPath": "${thumbnailPath ?: ""}",
            "tags": $tagsJson,
            "creatorId": "${artPiece.creatorId}",
            "timestamp": ${artPiece.timestamp}
        }
        """.trimIndent()

        metadataFile.writeText(artPieceJson)
    }
}