package com.rs.photoshare

import ArtPieceAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rs.photoshare.models.ArtPiece
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var artPiecesRecyclerView: RecyclerView
    private lateinit var artPieceAdapter: ArtPieceAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val localArtPieces = mutableListOf<ArtPiece>()  // Local cache for art pieces
    private var imageUri: Uri? = null  // Store picked image URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val userNameText: TextView = findViewById(R.id.userNameText)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val uploadArtButton: Button = findViewById(R.id.uploadArtButton)
        artPiecesRecyclerView = findViewById(R.id.artPiecesRecyclerView)

        artPiecesRecyclerView.layoutManager = LinearLayoutManager(this)

        loadArtPieces()

        // Set welcome text
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("name") ?: "User"
                    userNameText.text = "Welcome, $userName!"
                }
        }

        // Open image picker when upload button is clicked
        uploadArtButton.setOnClickListener {
            pickImageFromGallery()
        }

        // Logout logic
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Opens gallery for selecting an image
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    // Handle result from gallery picker
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data!!.data
            saveImageLocallyAndCreateArtPiece()
        }
    }

    // Save image locally and create new ArtPiece
    private fun saveImageLocallyAndCreateArtPiece() {
        val imageUri = imageUri ?: return
        val fileName = "art_${UUID.randomUUID()}.jpg"
        val file = File(filesDir, fileName)

        try {
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }

            // Add new art piece using local file path
            val newArtPiece = ArtPiece(
                artId = UUID.randomUUID().toString(),
                title = "Local Test Art",
                description = "This is a locally stored art piece.",
                imageUrl = file.absolutePath,
                tags = listOf("local", "art"),
                creatorId = auth.currentUser?.uid ?: "unknown",
                timestamp = System.currentTimeMillis()
            )

            localArtPieces.add(newArtPiece)
            updateRecyclerView()

            Toast.makeText(this, "Art piece added!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Load art pieces (in this case, only from local cache)
    private fun loadArtPieces() {
        updateRecyclerView()
    }

    // Refresh RecyclerView with current art pieces
    private fun updateRecyclerView() {
        artPieceAdapter = ArtPieceAdapter(localArtPieces)
        artPiecesRecyclerView.adapter = artPieceAdapter
    }
}
