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
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val titleInput = EditText(context).apply { hint = "Enter title" }
        val descriptionInput = EditText(context).apply { hint = "Enter description" }

        val tagSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, predefinedTags)
        }

        val customTagInput = EditText(context).apply {
            hint = "Enter custom tag (if selected)"
            visibility = View.GONE
        }

        tagSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                customTagInput.visibility = if (predefinedTags[position] == "Custom") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        layout.addView(titleInput)
        layout.addView(descriptionInput)
        layout.addView(tagSpinner)
        layout.addView(customTagInput)

        AlertDialog.Builder(context)
            .setTitle("Add Art Piece Details")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val selectedTag = if (tagSpinner.selectedItem == "Custom") {
                    customTagInput.text.toString().trim()
                } else {
                    tagSpinner.selectedItem.toString()
                }

                if (title.isEmpty() || description.isEmpty() || selectedTag.isEmpty()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                } else {
                    saveImageLocallyAndCreateArtPiece(title, description, selectedTag)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun saveImageLocallyAndCreateArtPiece(title: String, description: String, tag: String) {
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
                tags = listOf(tag),
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
        return """
        {
            "artId": "${artPiece.artId}",
            "title": "${artPiece.title}",
            "description": "${artPiece.description}",
            "imageUrl": "${artPiece.imageUrl}",
            "tags": ${artPiece.tags.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"")},
            "creatorId": "${artPiece.creatorId}",
            "timestamp": ${artPiece.timestamp}
        }
        """.trimIndent()
    }
}
