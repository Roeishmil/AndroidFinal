package com.rs.photoshare

import ArtPieceAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.rs.photoshare.models.ArtPiece
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var artPiecesRecyclerView: RecyclerView
    private lateinit var artPieceAdapter: ArtPieceAdapter
    private lateinit var imageUploadManager: ImageUploadManager


    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val localArtPieces = mutableListOf<ArtPiece>()

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val userNameText: TextView = findViewById(R.id.userNameText)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val uploadArtButton: Button = findViewById(R.id.uploadArtButton)
        val clearPostsButton: Button = findViewById(R.id.clearPostsButton)

        artPiecesRecyclerView = findViewById(R.id.artPiecesRecyclerView)
        artPiecesRecyclerView.layoutManager = LinearLayoutManager(this)

        imageUploadManager = ImageUploadManager(
            context = this,
            firestore = firestore,
            auth = auth,
            progressBar = findViewById(R.id.progressBar),   // NEW
            onArtPieceUploaded = { artPiece ->
                saveArtPieceToJson(artPiece)
                localArtPieces.add(artPiece)
                updateRecyclerView()
            },
        )


        uploadArtButton.setOnClickListener { imageUploadManager.startImagePicker(resultLauncher) }
        clearPostsButton.setOnClickListener { clearAllLocalArtPieces() }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loadArtPieces()

        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userNameText.text = "Welcome, ${document.getString("name") ?: "User"}!"
                }
        }
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        imageUploadManager.handleImageResult(result.resultCode, result.data)
    }

    private fun updateRecyclerView() {
        artPieceAdapter = ArtPieceAdapter(localArtPieces) { artPiece ->
            openArtPieceFragment(artPiece)
        }
        artPiecesRecyclerView.adapter = artPieceAdapter
    }

    private fun openArtPieceFragment(artPiece: ArtPiece) {
        findViewById<RecyclerView>(R.id.artPiecesRecyclerView).visibility = View.GONE
        findViewById<FrameLayout>(R.id.fragmentContainer).apply {
            visibility = View.VISIBLE
            layoutParams.height = 0  // Expand to full height (ConstraintLayout will stretch it)
            requestLayout()
        }

        val fragment = ArtPieceFragment.newInstance(artPiece)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }


    fun showRecyclerView() {
        findViewById<RecyclerView>(R.id.artPiecesRecyclerView).visibility = View.VISIBLE
        findViewById<FrameLayout>(R.id.fragmentContainer).visibility = View.GONE
    }




    private fun loadArtPieces() {
        localArtPieces.clear()

        val files = filesDir.listFiles { file -> file.extension == "jpg" } ?: emptyArray()

        for (imageFile in files) {
            val metadataFile = File(imageFile.parent, imageFile.nameWithoutExtension + ".json")

            if (metadataFile.exists()) {
                val artPiece = readArtPieceFromJson(metadataFile)
                if (artPiece != null) {
                    localArtPieces.add(artPiece)
                }
            }
        }

        updateRecyclerView()
    }

    private fun readArtPieceFromJson(file: File): ArtPiece? {
        return try {
            gson.fromJson(file.readText(), ArtPiece::class.java)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to read JSON for ${file.name}: ${e.message}")
            null
        }
    }

    private fun saveArtPieceToJson(artPiece: ArtPiece) {
        val metadataFile = File(filesDir, "art_${artPiece.artId}.json")
        metadataFile.writeText(gson.toJson(artPiece))
    }

    private fun clearAllLocalArtPieces() {
        filesDir.listFiles { file -> file.extension in setOf("jpg", "json") }?.forEach { it.delete() }
        localArtPieces.clear()
        updateRecyclerView()
    }
}
