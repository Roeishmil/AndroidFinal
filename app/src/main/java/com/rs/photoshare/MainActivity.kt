package com.rs.photoshare

import ArtPieceAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
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

    private lateinit var tagFilterLayout: LinearLayout
    private lateinit var applyFilterButton: Button
    private lateinit var clearFilterButton: Button
    private lateinit var toggleFiltersButton: Button
    private lateinit var filterContainer: LinearLayout
    private val selectedTags = mutableSetOf<String>()

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
        val profileButton: Button = findViewById(R.id.profileButton)

        artPiecesRecyclerView = findViewById(R.id.artPiecesRecyclerView)
        artPiecesRecyclerView.layoutManager = LinearLayoutManager(this)

        imageUploadManager = ImageUploadManager(
            context = this,
            firestore = firestore,
            auth = auth,
            progressBar = findViewById(R.id.progressBar),
            onArtPieceUploaded = { artPiece ->
                saveArtPieceToJson(artPiece)
                localArtPieces.add(artPiece)
                updateRecyclerView()
                setupTagFilters() // Refresh tag filters when a new art piece is added
            },
        )

        profileButton.setOnClickListener {
            openUserProfileFragment()
        }

        // Initialize tag filtering components
        filterContainer = findViewById(R.id.filterContainer)
        tagFilterLayout = findViewById(R.id.tagFilterLayout)
        applyFilterButton = findViewById(R.id.applyFilterButton)
        clearFilterButton = findViewById(R.id.clearFilterButton)
        toggleFiltersButton = findViewById(R.id.toggleFiltersButton)

        // Set up toggle filters button
        toggleFiltersButton.setOnClickListener {
            toggleFiltersVisibility()
        }

        applyFilterButton.setOnClickListener {
            filterArtPieces()
        }

        clearFilterButton.setOnClickListener {
            // Clear all checkboxes
            tagFilterLayout.children.forEach { view ->
                if (view is CheckBox) {
                    view.isChecked = false
                }
            }
            selectedTags.clear()
            updateRecyclerView() // Show all art pieces
        }

        uploadArtButton.setOnClickListener { imageUploadManager.startImagePicker(resultLauncher) }
        clearPostsButton.setOnClickListener { clearAllLocalArtPieces() }

        val myArtButton: Button = findViewById(R.id.myArtButton)
        myArtButton.setOnClickListener {
            openUserArtFragment()
        }

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

    private fun toggleFiltersVisibility() {
        if (filterContainer.visibility == View.VISIBLE) {
            filterContainer.visibility = View.GONE
            toggleFiltersButton.text = "Show Filters"
        } else {
            filterContainer.visibility = View.VISIBLE
            toggleFiltersButton.text = "Hide Filters"
        }
    }

    fun updateUserHeader(newName: String) {
        val userNameTextView = findViewById<TextView>(R.id.userNameText)
        userNameTextView.text = "Welcome, $newName!"
    }

    fun hideButtons() {
        findViewById<Button>(R.id.uploadArtButton)?.visibility = View.GONE
        findViewById<Button>(R.id.clearPostsButton)?.visibility = View.GONE
        findViewById<Button>(R.id.logoutButton)?.visibility = View.GONE
        findViewById<Button>(R.id.profileButton)?.visibility = View.GONE  // Hide Profile button
    }

    fun showButtons() {
        findViewById<Button>(R.id.uploadArtButton)?.visibility = View.VISIBLE
        findViewById<Button>(R.id.clearPostsButton)?.visibility = View.VISIBLE
        findViewById<Button>(R.id.logoutButton)?.visibility = View.VISIBLE
        findViewById<Button>(R.id.profileButton)?.visibility = View.VISIBLE  // Show Profile button
    }

    private fun setupTagFilters() {
        // Clear existing views
        tagFilterLayout.removeAllViews()
        selectedTags.clear()

        // Get unique tags from all posts
        val uniqueTags = localArtPieces.flatMap { it.tags }.distinct().sorted()

        if (uniqueTags.isEmpty()) {
            // Add a message if no tags exist
            val textView = TextView(this)
            textView.text = "No tags available"
            textView.setPadding(8, 8, 8, 8)
            tagFilterLayout.addView(textView)
            return
        }

        // Add a checkbox for each unique tag
        uniqueTags.forEach { tag ->
            val checkBox = CheckBox(this)
            checkBox.text = tag
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedTags.add(tag)
                } else {
                    selectedTags.remove(tag)
                }
            }
            tagFilterLayout.addView(checkBox)
        }
    }

    private fun filterArtPieces() {
        if (selectedTags.isEmpty()) {
            updateRecyclerView() // Show all art pieces
        } else {
            // Filter art pieces that have ANY of the selected tags
            val filteredList = localArtPieces.filter { artPiece ->
                artPiece.tags.any { tag -> selectedTags.contains(tag) }
            }
            artPieceAdapter.updateList(filteredList)
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
        setupTagFilters() // Setup tag filters after loading art pieces
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
        setupTagFilters() // Refresh tag filters after clearing all art pieces
    }

    fun refreshArtPieces(forceImageReload: Boolean = false) {
        loadArtPieces()
        setupTagFilters() // Refresh tag filters when art pieces are refreshed

        // Force Picasso to reload images by clearing the cache
        if (forceImageReload) {
            artPieceAdapter.notifyDataSetChanged()
        }
    }

    private fun openUserProfileFragment() {
        findViewById<RecyclerView>(R.id.artPiecesRecyclerView).visibility = View.GONE
        findViewById<FrameLayout>(R.id.fragmentContainer).visibility = View.VISIBLE

        val fragment = UserProfileFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openUserArtFragment() {
        findViewById<RecyclerView>(R.id.artPiecesRecyclerView).visibility = View.GONE
        findViewById<FrameLayout>(R.id.fragmentContainer).visibility = View.VISIBLE

        val fragment = MyArtFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object
}