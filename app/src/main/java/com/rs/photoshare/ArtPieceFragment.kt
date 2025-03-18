package com.rs.photoshare

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.rs.photoshare.managers.AuthManager
import com.rs.photoshare.models.ArtPiece
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream

class ArtPieceFragment : Fragment() {

    private lateinit var artPiece: ArtPiece
    private lateinit var authManager: AuthManager
    private var newImageUri: Uri? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                newImageUri = result.data?.data
                Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If using Safe Args, you'd do:
        // artPiece = ArtPieceFragmentArgs.fromBundle(requireArguments()).artPiece
        // Otherwise, get the parcelable from the Bundle directly:
        artPiece = requireArguments().getParcelable("artPiece")!!

        authManager = AuthManager()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_art_piece, container, false)

        val titleTextView = view.findViewById<TextView>(R.id.artPieceTitleView)
        val descriptionTextView = view.findViewById<TextView>(R.id.artPieceDescriptionView)
        val tagTextView = view.findViewById<TextView>(R.id.artPieceTagView)
        val imageView = view.findViewById<ImageView>(R.id.artPieceImageView)
        val editButton = view.findViewById<Button>(R.id.editButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val backButton = view.findViewById<Button>(R.id.backButton)

        titleTextView.text = artPiece.title
        descriptionTextView.text = artPiece.description
        tagTextView.text = artPiece.tags.firstOrNull() ?: "No Tag"
        // Load image differently based on source
        if (artPiece.imageUrl.startsWith("http")) {
            // Cloudinary URL
            Picasso.get().load(artPiece.imageUrl).into(imageView)
        } else {
            // Local file
            imageView.setImageBitmap(BitmapFactory.decodeFile(artPiece.imageUrl))
        }
        //imageView.setImageBitmap(BitmapFactory.decodeFile(artPiece.imageUrl))

        val currentUserId = authManager.getCurrentUserId()
        if (artPiece.creatorId != currentUserId) {
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
        }

        editButton.setOnClickListener { showEditDialog() }
        deleteButton.setOnClickListener { showDeleteConfirmationDialog() }
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        return view
    }

    private fun showEditDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val titleInput = EditText(context).apply {
            hint = "Enter title"
            setText(artPiece.title)
        }
        val descriptionInput = EditText(context).apply {
            hint = "Enter description"
            setText(artPiece.description)
        }
        val changeImageButton = Button(context).apply {
            text = "Change Image"
            setOnClickListener { openImagePicker() }
        }

        layout.addView(titleInput)
        layout.addView(descriptionInput)
        layout.addView(changeImageButton)

        AlertDialog.Builder(context)
            .setTitle("Edit Post")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = titleInput.text.toString().trim()
                val newDescription = descriptionInput.text.toString().trim()
                if (newTitle.isEmpty() || newDescription.isEmpty()) {
                    Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    updatePost(newTitle, newDescription)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun updatePost(newTitle: String, newDescription: String) {
        val metadataFile = File(requireContext().filesDir, "art_${artPiece.artId}.json")
        if (metadataFile.exists()) {
            var newImagePath = artPiece.imageUrl

            // If a new image was selected, save it and update the path
            newImageUri?.let { uri ->
                val savedPath = saveImageLocally(uri, artPiece.artId)
                if (savedPath != null) {
                    newImagePath = savedPath
                } else {
                    Toast.makeText(requireContext(), "Failed to save new image", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val updatedArtPiece = artPiece.copy(
                title = newTitle,
                description = newDescription,
                imageUrl = newImagePath
            )

            metadataFile.writeText(Gson().toJson(updatedArtPiece))

            // If the old image is different, delete it
            if (newImageUri != null && artPiece.imageUrl != newImagePath) {
                File(artPiece.imageUrl).delete()
            }

            Toast.makeText(requireContext(), "Post updated", Toast.LENGTH_SHORT).show()

            (activity as? MainActivity)?.refreshArtPieces(forceImageReload = true)
            findNavController().navigateUp()
        }
    }

    private fun saveImageLocally(uri: Uri, artId: String): String? {
        val imageFile = File(requireContext().filesDir, "art_$artId.jpg")
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
            imageFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ -> deletePost() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost() {
        val imageFile = File(artPiece.imageUrl)
        val metadataFile = File(requireContext().filesDir, "art_${artPiece.artId}.json")

        imageFile.delete()
        metadataFile.delete()

        Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
        (activity as? MainActivity)?.refreshArtPieces()
        findNavController().navigateUp()
    }

    companion object {
        fun newInstance(artPiece: ArtPiece) = ArtPieceFragment().apply {
            arguments = Bundle().apply {
                putParcelable("artPiece", artPiece)
            }
        }
    }
}
