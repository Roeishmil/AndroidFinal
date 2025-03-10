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
import com.google.firebase.firestore.FirebaseFirestore
import com.rs.photoshare.models.ArtPiece
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ImageUploadManager(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val progressBar: ProgressBar?,
    private val onArtPieceUploaded: (ArtPiece) -> Unit
) {

    private var imageUri: Uri? = null

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
                    saveImageLocallyAndCreateArtPiece(title, description, finalTags)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun saveImageLocallyAndCreateArtPiece(title: String, description: String, tags: List<String>) {
        progressBar?.visibility = View.VISIBLE  // Show spinner immediately

        val imageUri = imageUri ?: return
        val artId = UUID.randomUUID().toString()
        val imageFile = File(context.filesDir, "art_${artId}.jpg")
        val metadataFile = File(context.filesDir, "art_${artId}.json")

        try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }

            val newArtPiece = ArtPiece(
                artId = artId,
                title = title,
                description = description,
                imageUrl = imageFile.absolutePath,
                tags = tags,
                creatorId = auth.currentUser?.uid ?: "unknown",
                timestamp = System.currentTimeMillis()
            )

            metadataFile.writeText(toJson(newArtPiece))

            // Simulate success callback
            onArtPieceUploaded(newArtPiece)

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            progressBar?.visibility = View.GONE  // Always hide spinner when done
        }
    }

    private fun toJson(artPiece: ArtPiece): String {
        val tagsJson = artPiece.tags.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"")

        return """
        {
            "artId": "${artPiece.artId}",
            "title": "${artPiece.title}",
            "description": "${artPiece.description}",
            "imageUrl": "${artPiece.imageUrl}",
            "tags": $tagsJson,
            "creatorId": "${artPiece.creatorId}",
            "timestamp": ${artPiece.timestamp}
        }
        """.trimIndent()
    }
}