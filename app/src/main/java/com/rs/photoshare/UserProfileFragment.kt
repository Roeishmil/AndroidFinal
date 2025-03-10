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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.rs.photoshare.models.User
import java.io.File
import java.io.FileOutputStream
import android.os.Handler
import android.os.Looper

class UserProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentUser: User? = null
    private var newProfileImageUri: Uri? = null

    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var editNameButton: Button
    private lateinit var changeProfileImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                newProfileImageUri = result.data?.data
                profileImageView.setImageURI(newProfileImageUri)  // Show new image immediately
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)

        profileImageView = view.findViewById(R.id.profileImageView)
        userNameTextView = view.findViewById(R.id.userNameTextView)
        editNameButton = view.findViewById(R.id.editNameButton)
        changeProfileImageButton = view.findViewById(R.id.changeProfileImageButton)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        // Hide unrelated buttons from MainActivity (including View Profile button)
        (activity as? MainActivity)?.hideButtons()

        // Load user data
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    currentUser = document.toObject(User::class.java)
                    userNameTextView.text = currentUser?.name ?: "Unknown User"
                    loadProfileImage(currentUser?.profilePictureUrl)
                }
        }

        editNameButton.setOnClickListener { showEditNameDialog() }
        changeProfileImageButton.setOnClickListener { openImagePicker() }
        saveButton.setOnClickListener { saveUserProfile() }
        cancelButton.setOnClickListener { goBackToMain(immediate = true) }

        return view
    }

    private fun showEditNameDialog() {
        val context = requireContext()
        val input = EditText(context).apply {
            hint = "Enter new name"
            setText(currentUser?.name)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    userNameTextView.text = newName  // Update UI immediately
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val newName = userNameTextView.text.toString().trim()

        var newProfileImagePath = currentUser?.profilePictureUrl ?: ""

        // Save new profile picture if selected
        newProfileImageUri?.let { uri ->
            val savedPath = saveProfileImageLocally(uri, userId)
            if (savedPath != null) {
                newProfileImagePath = savedPath
            } else {
                Toast.makeText(requireContext(), "Failed to save new profile image", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val updatedUser = User(
            userId = userId,
            name = newName,
            email = currentUser?.email ?: "",
            profilePictureUrl = newProfileImagePath,
            uploadedArtPiece = currentUser?.uploadedArtPiece ?: emptyList()
        )

        // Update UI first to ensure immediate response
        (activity as? MainActivity)?.updateUserHeader(newName)

        // Show a saving toast
        Toast.makeText(context, "Saving profile...", Toast.LENGTH_SHORT).show()

        // Then perform the Firestore update
        firestore.collection("users").document(userId).set(updatedUser)
            .addOnSuccessListener {
                // Success message
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()

                // Force navigation back to main after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    goBackToMain(immediate = true)
                }, 500) // Half-second delay to ensure the toast is seen
            }
            .addOnFailureListener { e ->
                // Show error but still go back
                Toast.makeText(requireContext(), "Error updating profile: ${e.message}", Toast.LENGTH_LONG).show()
                goBackToMain(immediate = true)
            }
    }

    private fun goBackToMain(immediate: Boolean = false) {
        // Make sure we're attached to an activity
        if (!isAdded()) return

        try {
            // Update the MainActivity's UI
            val mainActivity = activity as? MainActivity
            mainActivity?.showButtons()

            if (immediate) {
                // More direct approach for the Save button
                // Clear the back stack completely and force show the main view
                parentFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                mainActivity?.showRecyclerView()
                mainActivity?.refreshArtPieces()
            } else {
                // Standard approach for the Cancel button
                parentFragmentManager.popBackStack()
                mainActivity?.showRecyclerView()
                mainActivity?.refreshArtPieces()
            }
        } catch (e: Exception) {
            // Last resort: recreate the activity
            Toast.makeText(context, "Returning to main view", Toast.LENGTH_SHORT).show()
            activity?.recreate()
        }
    }

    private fun saveProfileImageLocally(uri: Uri, userId: String): String? {
        val imageFile = File(requireContext().filesDir, "profile_$userId.jpg")

        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
            imageFile.name
        } catch (e: Exception) {
            null
        }
    }

    private fun loadProfileImage(imageFileName: String?) {
        if (!imageFileName.isNullOrEmpty()) {
            val imageFile = File(requireContext().filesDir, imageFileName)
            if (imageFile.exists()) {
                profileImageView.setImageBitmap(BitmapFactory.decodeFile(imageFile.absolutePath))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure buttons are shown again if fragment is destroyed
        (activity as? MainActivity)?.showButtons()
    }

    companion object {
        fun newInstance() = UserProfileFragment()
    }
}