package com.rs.photoshare

import ArtPieceAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.rs.photoshare.models.ArtPiece
import java.io.File

class MainActivity : AppCompatActivity(){

    // Lateinit properties
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
        // Initialize Cloudinary
        CloudinaryManager.initialize(this)

        // --- 1) Initialize all views first ---
        val userNameText: TextView = findViewById(R.id.userNameText)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val uploadArtButton: Button = findViewById(R.id.uploadArtButton)
        val clearPostsButton: Button = findViewById(R.id.clearPostsButton)
        val profileButton: Button = findViewById(R.id.profileButton)
        val myArtButton: Button = findViewById(R.id.myArtButton)

        artPiecesRecyclerView = findViewById(R.id.artPiecesRecyclerView)
        artPiecesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Tag filter UI
        filterContainer = findViewById(R.id.filterContainer)
        tagFilterLayout = findViewById(R.id.tagFilterLayout)
        applyFilterButton = findViewById(R.id.applyFilterButton)
        clearFilterButton = findViewById(R.id.clearFilterButton)
        toggleFiltersButton = findViewById(R.id.toggleFiltersButton)

        toggleFiltersButton.setOnClickListener { toggleFiltersVisibility() }
        applyFilterButton.setOnClickListener { filterArtPieces() }
        clearFilterButton.setOnClickListener {
            // Clear all checkboxes
            tagFilterLayout.children.forEach { view ->
                if (view is CheckBox) {
                    view.isChecked = false
                }
            }
            selectedTags.clear()
            updateRecyclerView() // Show all art
        }

        imageUploadManager = ImageUploadManager(
            context = this,
            auth = auth,
            progressBar = findViewById(R.id.progressBar),
            onArtPieceUploaded = { artPiece ->
                // Once an art piece is uploaded, save it locally:
                saveArtPieceToJson(artPiece)
                localArtPieces.add(artPiece)
                updateRecyclerView()
                setupTagFilters()
            },
            fragmentManager = supportFragmentManager
        )

        // --- 2) Set up button listeners (profile, etc.) ---
        profileButton.setOnClickListener { openUserProfileFragment() }
        myArtButton.setOnClickListener { openUserArtFragment() }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Modified to show the image source selection dialog
        uploadArtButton.setOnClickListener {
            imageUploadManager.showImageSourceSelectionDialog(resultLauncher)
        }

        clearPostsButton.setOnClickListener { clearAllLocalArtPieces() }

        // --- 3) Load local art & set up user name ---
        loadArtPieces()

        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userNameText.text = "Welcome, ${document.getString("name") ?: "User"}!"
                }
        }

        // --- 4) Attach NavController listener for auto-hide/show UI ---
        val navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // If your nav_graph's "main" is homeFragment:
                R.id.homeFragment -> {
                    showMainButtons()
                    showRecyclerView()
                }
                else -> {
                    hideMainButtons()
                    hideRecyclerView()
                }
            }
        }
    }


    // ~~~~~~~~~~~~~~~ Navigation-related methods ~~~~~~~~~~~~~~~

    private fun openUserProfileFragment() {
        val navController = findNavController(R.id.nav_host_fragment)
        // If already on userProfileFragment, do nothing
        if (navController.currentDestination?.id == R.id.userProfileFragment) {
            return
        }
        hideRecyclerView()
        navController.navigate(R.id.userProfileFragment)
    }

    private fun openUserArtFragment() {
        val navController = findNavController(R.id.nav_host_fragment)
        // If already on myArtFragment, do nothing
        if (navController.currentDestination?.id == R.id.myArtFragment) {
            return
        }
        hideRecyclerView()
        navController.navigate(R.id.myArtFragment)
    }

    fun openArtPieceFragment(artPiece: ArtPiece) {
        val navController = findNavController(R.id.nav_host_fragment)
        // Optional: If you want to avoid opening the same fragment multiple times, check currentDest

        // Hide the main screen
        hideRecyclerView()

        val bundle = Bundle().apply {
            putParcelable("artPiece", artPiece)
        }
        navController.navigate(R.id.artPieceFragment, bundle)
    }

    // ~~~~~~~~~~~~~~~ Show/Hide Methods ~~~~~~~~~~~~~~~

    fun showMainButtons() {
        findViewById<Button>(R.id.uploadArtButton).visibility = View.VISIBLE
        findViewById<Button>(R.id.clearPostsButton).visibility = View.VISIBLE
        findViewById<Button>(R.id.myArtButton).visibility = View.VISIBLE
        findViewById<Button>(R.id.toggleFiltersButton).visibility = View.VISIBLE
    }

    fun hideMainButtons() {
        findViewById<Button>(R.id.uploadArtButton).visibility = View.GONE
        findViewById<Button>(R.id.clearPostsButton).visibility = View.GONE
        findViewById<Button>(R.id.myArtButton).visibility = View.GONE
        findViewById<Button>(R.id.toggleFiltersButton).visibility = View.GONE
    }

    fun showRecyclerView() {
        artPiecesRecyclerView.visibility = View.VISIBLE
        findViewById<View>(R.id.nav_host_fragment).visibility = View.GONE
    }

    fun hideRecyclerView() {
        artPiecesRecyclerView.visibility = View.GONE
        findViewById<View>(R.id.nav_host_fragment).visibility = View.VISIBLE
    }

    // ~~~~~~~~~~~~~~~ Image Upload Result ~~~~~~~~~~~~~~~

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        imageUploadManager.handleImageResult(result.resultCode, result.data)
    }

    // ~~~~~~~~~~~~~~~ Filtering + Loading Art Methods ~~~~~~~~~~~~~~~

    private fun toggleFiltersVisibility() {
        if (filterContainer.visibility == View.VISIBLE) {
            filterContainer.visibility = View.GONE
            toggleFiltersButton.text = "Show Filters"
        } else {
            filterContainer.visibility = View.VISIBLE
            toggleFiltersButton.text = "Hide Filters"
        }
    }

    private fun filterArtPieces() {
        if (selectedTags.isEmpty()) {
            // Show all, sorted by rating
            artPieceAdapter.updateList(sortArtPiecesByRating(localArtPieces))
        } else {
            val filteredList = localArtPieces.filter { piece ->
                piece.tags.any { tag -> selectedTags.contains(tag) }
            }
            artPieceAdapter.updateList(sortArtPiecesByRating(filteredList))
        }
    }

    private fun setupTagFilters() {
        tagFilterLayout.removeAllViews()
        selectedTags.clear()

        val uniqueTags = localArtPieces.flatMap { it.tags }.distinct().sorted()
        if (uniqueTags.isEmpty()) {
            val textView = TextView(this)
            textView.text = "No tags available"
            textView.setPadding(8, 8, 8, 8)
            tagFilterLayout.addView(textView)
            return
        }

        for (tag in uniqueTags) {
            val checkBox = CheckBox(this).apply {
                text = tag
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTags.add(tag)
                    } else {
                        selectedTags.remove(tag)
                    }
                }
            }
            tagFilterLayout.addView(checkBox)
        }
    }

    private fun loadArtPieces() {
        localArtPieces.clear()
        // Look for JSON files instead of JPG files
        val files = filesDir.listFiles { file -> file.extension == "json" && file.name.startsWith("art_") } ?: emptyArray()

        for (metadataFile in files) {
            val artPiece = readArtPieceFromJson(metadataFile)
            if (artPiece != null) {
                localArtPieces.add(artPiece)
            }
        }
        updateRecyclerView()
        setupTagFilters()
    }

    private fun clearAllLocalArtPieces() {
        filesDir.listFiles { file -> file.extension in setOf("jpg", "json") }?.forEach { it.delete() }
        localArtPieces.clear()
        updateRecyclerView()
        setupTagFilters()
    }

    private fun readArtPieceFromJson(file: File): ArtPiece? {
        return try {
            gson.fromJson(file.readText(), ArtPiece::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveArtPieceToJson(artPiece: ArtPiece) {
        val metadataFile = File(filesDir, "art_${artPiece.artId}.json")
        metadataFile.writeText(gson.toJson(artPiece))
    }

    private fun setupArtPieceAdapter() {
        artPieceAdapter = ArtPieceAdapter(
            sortArtPiecesByRating(localArtPieces),
            onItemClick = { artPiece -> openArtPieceFragment(artPiece) },
            onLikeClick = { artPiece -> updateArtPieceRating(artPiece, true) },
            onDislikeClick = { artPiece -> updateArtPieceRating(artPiece, false) }
        )
        artPiecesRecyclerView.adapter = artPieceAdapter
    }

     fun updateArtPieceRating(artPiece: ArtPiece, isLike: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val metadataFile = File(filesDir, "art_${artPiece.artId}.json")

        if (!metadataFile.exists()) {
            Toast.makeText(this, "Cannot update rating for this post", Toast.LENGTH_SHORT).show()
            return
        }

        val likedBy = artPiece.likedBy.toMutableList()
        val dislikedBy = artPiece.dislikedBy.toMutableList()

        var likes = artPiece.likes
        var dislikes = artPiece.dislikes

        // Update liked/disliked lists and counts
        if (isLike) {
            if (likedBy.contains(currentUserId)) {
                // Already liked, remove like
                likedBy.remove(currentUserId)
                likes--
            } else {
                // Add like and remove dislike if present
                likedBy.add(currentUserId)
                likes++

                if (dislikedBy.contains(currentUserId)) {
                    dislikedBy.remove(currentUserId)
                    dislikes--
                }
            }
        } else {
            if (dislikedBy.contains(currentUserId)) {
                // Already disliked, remove dislike
                dislikedBy.remove(currentUserId)
                dislikes--
            } else {
                // Add dislike and remove like if present
                dislikedBy.add(currentUserId)
                dislikes++

                if (likedBy.contains(currentUserId)) {
                    likedBy.remove(currentUserId)
                    likes--
                }
            }
        }

        // Create updated art piece
        val updatedArtPiece = artPiece.copy(
            likes = likes,
            dislikes = dislikes,
            likedBy = likedBy,
            dislikedBy = dislikedBy
        )

        // Save updated art piece
        metadataFile.writeText(gson.toJson(updatedArtPiece))

        // Update local list
        val index = localArtPieces.indexOfFirst { it.artId == artPiece.artId }
        if (index != -1) {
            localArtPieces[index] = updatedArtPiece
        }

        // Refresh the sorted list
        artPieceAdapter.updateList(sortArtPiecesByRating(localArtPieces))
    }

    // Sort art pieces by rating (likes - dislikes)
    private fun sortArtPiecesByRating(pieces: List<ArtPiece>): List<ArtPiece> {
        return pieces.sortedByDescending { it.getRatingScore() }
    }

    private fun updateRecyclerView() {
        setupArtPieceAdapter()
    }

    fun getFragmentContainerId(): Int {
        return R.id.fragment_container
    }
    
    fun refreshArtPieces(forceImageReload: Boolean = false) {
        loadArtPieces()
        setupTagFilters()
        if (forceImageReload) {
            artPieceAdapter.notifyDataSetChanged()
        }
    }
}