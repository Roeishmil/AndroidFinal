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
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.rs.photoshare.api.PhotoApiService
import com.rs.photoshare.fragments.TagSuggestionFragment
import com.rs.photoshare.models.ArtPiece
import com.rs.photoshare.models.Photo
import com.rs.photoshare.services.TagSuggestionService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class ImageUploadManager(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val progressBar: ProgressBar?,
    private val fragmentManager: FragmentManager?,
    private val onArtPieceUploaded: (ArtPiece) -> Unit
) {

    private var imageUri: Uri? = null
    private val cloudinaryManager = CloudinaryManager(context)
    private val predefinedTags = listOf("Warrior", "Wizard", "Doctor", "Engineer", "Custom")
    private var selectedTags = mutableListOf<String>()
    private var tagSuggestionService: TagSuggestionService? = null

    init {
        tagSuggestionService = TagSuggestionService(context)
    }

    // Retrofit instance for the PhotoApiService
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://picsum.photos/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val photoApiService = retrofit.create(PhotoApiService::class.java)

    // Method to show image source selection dialog
    fun showImageSourceSelectionDialog(launcher: ActivityResultLauncher<Intent>) {
        val options = arrayOf("Device Gallery", "Online Photos")

        AlertDialog.Builder(context)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startImagePicker(launcher)
                    1 -> showOnlinePhotoSelectionDialog(launcher)
                }
            }
            .show()
    }

    // Original method to pick from device gallery
    fun startImagePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        launcher.launch(intent)
    }

    // New method to display and select online photos
    private fun showOnlinePhotoSelectionDialog(launcher: ActivityResultLauncher<Intent>) {
        val dialogView = View.inflate(context, R.layout.dialog_online_photos, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.onlinePhotosRecyclerView)
        val loadingProgressBar = dialogView.findViewById<ProgressBar>(R.id.loadingProgressBar)

        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Select an Online Photo")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        loadingProgressBar.visibility = View.VISIBLE

        // Fetch photos from API
        photoApiService.getPhotos(page = 1, limit = 20).enqueue(object : Callback<List<Photo>> {
            override fun onResponse(call: Call<List<Photo>>, response: Response<List<Photo>>) {
                loadingProgressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val photos = response.body() ?: emptyList()

                    val adapter = OnlinePhotoAdapter(photos) { selectedPhoto ->
                        dialog.dismiss()

                        progressBar?.visibility = View.VISIBLE
                        Thread {
                            try {
                                // Download the image to a temporary file
                                val tempFile = downloadImageToTempFile(selectedPhoto.download_url)

                                // Convert to URI
                                val contentUri = Uri.fromFile(tempFile)

                                // Update UI on main thread
                                (context as? Activity)?.runOnUiThread {
                                    progressBar?.visibility = View.GONE
                                    imageUri = contentUri
                                    showAddDetailsDialog()
                                }
                            } catch (e: Exception) {
                                (context as? Activity)?.runOnUiThread {
                                    progressBar?.visibility = View.GONE
                                    Toast.makeText(context, "Failed to download image: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.start()
                    }

                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(context, "Failed to load photos: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Photo>>, t: Throwable) {
                loadingProgressBar.visibility = View.GONE
                Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        dialog.show()
    }

    // Download image from URL to a temporary file
    private fun downloadImageToTempFile(imageUrl: String): File {
        val url = URL(imageUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()

        val input: InputStream = connection.inputStream
        val tempFile = File.createTempFile("retrofit_image", ".jpg", context.cacheDir)

        FileOutputStream(tempFile).use { output ->
            val buffer = ByteArray(4 * 1024) // 4k buffer
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.flush()
        }

        return tempFile
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

        selectedTags.clear()
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

        // Add AI tag suggestion button
        val suggestTagsButton = Button(context).apply {
            text = "Get AI Tag Suggestions"
            setOnClickListener {
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                if (title.isEmpty() && description.isEmpty()) {
                    Toast.makeText(context, "Please enter a title or description first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Store the custom tag input field reference to update it later
                val customTagRef = customTagInput

                // Show the tag suggestion fragment
                showTagSuggestionFragment(title, description)
            }
        }

        layout.addView(titleInput)
        layout.addView(descriptionInput)
        layout.addView(tagLayout)
        layout.addView(customTagInput)
        layout.addView(suggestTagsButton)

        val dialog = AlertDialog.Builder(context)
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
            .create()

        dialog.show()
    }

    private fun showTagSuggestionFragment(title: String, description: String) {
        fragmentManager?.let { fm ->
            // Create input text for tag suggestions
            val inputText = if (title.isNotEmpty() && description.isNotEmpty()) {
                "$title: $description"
            } else if (title.isNotEmpty()) {
                title
            } else {
                description
            }

            // Make sure the fragment container is visible in MainActivity
            if (context is MainActivity) {
                (context as MainActivity).hideRecyclerView()
            }

            // Create the TagSuggestionFragment with your existing implementation
            val fragment = TagSuggestionFragment.newInstance(inputText)

            // Store a reference to this ImageUploadManager instance in the fragment
            val field = TagSuggestionFragment::class.java.getDeclaredField("uploadManager")
            field.isAccessible = true
            field.set(fragment, this)

            fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } ?: run {
            Toast.makeText(context, "Cannot show tag suggestions at this time", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var customTagField: EditText

    fun updateSelectedTags(tags: List<String>) {
        // Store the suggested tags as a single comma-separated string
        val suggestedTagsText = tags.joinToString(", ")

        // Find the custom tag input in the current dialog
        try {
            // Find the dialog that's currently showing
            val currentDialog = findCurrentDialog()
            if (currentDialog != null) {
                // Find the custom tag EditText in the dialog
                val customTagInput = findCustomTagInput(currentDialog)
                if (customTagInput != null) {
                    // Update the custom tag field with the suggested tags
                    customTagInput.setText(suggestedTagsText)
                } else {
                    Toast.makeText(context, "Could not find custom tag field", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            //Log.e("ImageUploadManager", "Error updating tags: ${e.message}", e)
        }
    }

    // Helper method to find the current showing dialog
    private fun findCurrentDialog(): AlertDialog? {
        try {
            val field = AlertDialog::class.java.getDeclaredField("mShowing")
            field.isAccessible = true

            // Get the Activity's Window DecorView
            val activity = context as? Activity
            val decorView = activity?.window?.decorView

            // Find all AlertDialog instances within the current window
            if (decorView != null) {
                return findAlertDialogInView(decorView)
            }
        } catch (e: Exception) {
            //Log.e("ImageUploadManager", "Error finding dialog: ${e.message}", e)
        }
        return null
    }

    // Recursively search for AlertDialog in the view hierarchy
    private fun findAlertDialogInView(view: View): AlertDialog? {
        if (view.tag is AlertDialog) {
            return view.tag as AlertDialog
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val dialog = findAlertDialogInView(child)
                if (dialog != null) {
                    return dialog
                }
            }
        }

        return null
    }

    // Helper method to find the custom tag input in the dialog
    private fun findCustomTagInput(dialog: AlertDialog): EditText? {
        val contentView = dialog.findViewById<ViewGroup>(android.R.id.content)
        return findEditTextWithHint(contentView, "Enter custom tag (optional)")
    }

    // Recursively search for EditText with specific hint
    private fun findEditTextWithHint(viewGroup: ViewGroup?, hint: String): EditText? {
        if (viewGroup == null) return null

        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (child is EditText && child.hint == hint) {
                return child
            } else if (child is ViewGroup) {
                val result = findEditTextWithHint(child, hint)
                if (result != null) {
                    return result
                }
            }
        }

        return null
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
                    timestamp = System.currentTimeMillis(),
                    likes = 0,
                    dislikes = 0,
                    likedBy = listOf(),
                    dislikedBy = listOf()
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