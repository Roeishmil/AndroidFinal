package com.rs.photoshare

import ArtPieceAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rs.photoshare.data.AppDatabase
import com.rs.photoshare.models.ArtPiece
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var artPiecesRecyclerView: RecyclerView
    private lateinit var artPieceAdapter: ArtPieceAdapter
    private lateinit var imageUploadManager: ImageUploadManager

    // Filter UI
    private lateinit var tagFilterLayout: LinearLayout
    private lateinit var applyFilterButton: Button
    private lateinit var clearFilterButton: Button
    private lateinit var toggleFiltersButton: Button
    private lateinit var filterContainer: LinearLayout
    private val selectedTags = mutableSetOf<String>()

    // Firebase (for user info) and Room (for art posts)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var artPieceDao: com.rs.photoshare.data.ArtPieceDao

    // Holds all currently loaded art pieces from local Room
    private val localArtPieces = mutableListOf<ArtPiece>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        CloudinaryManager.initialize(this)

        // --- 1) Find Views ---
        val userNameText: TextView = findViewById(R.id.userNameText)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val uploadArtButton: Button = findViewById(R.id.uploadArtButton)
        val clearPostsButton: Button = findViewById(R.id.clearPostsButton)
        val profileButton: Button = findViewById(R.id.profileButton)
        val myArtButton: Button = findViewById(R.id.myArtButton)
        val downloadedImagesButton: Button = findViewById(R.id.downloadedImagesButton) // NEW button

        // Filter UI
        filterContainer = findViewById(R.id.filterContainer)
        tagFilterLayout = findViewById(R.id.tagFilterLayout)
        applyFilterButton = findViewById(R.id.applyFilterButton)
        clearFilterButton = findViewById(R.id.clearFilterButton)
        toggleFiltersButton = findViewById(R.id.toggleFiltersButton)

        // --- 2) Initialize Room Database + DAO ---
        val database = AppDatabase.getDatabase(this)
        artPieceDao = database.artPieceDao()

        // --- 3) Set up RecyclerView ---
        artPiecesRecyclerView = findViewById(R.id.artPiecesRecyclerView)
        artPiecesRecyclerView.layoutManager = LinearLayoutManager(this)

        toggleFiltersButton.setOnClickListener { toggleFiltersVisibility() }
        applyFilterButton.setOnClickListener { filterArtPieces() }
        clearFilterButton.setOnClickListener {
            tagFilterLayout.children.forEach { view ->
                if (view is CheckBox) view.isChecked = false
            }
            selectedTags.clear()
            updateRecyclerView()
        }

        // --- 4) Set up Image Upload Manager ---
        imageUploadManager = ImageUploadManager(
            context = this,
            auth = auth,
            progressBar = findViewById(R.id.progressBar),
            onArtPieceUploaded = { artPiece ->
                // Save to Room and update the local list
                CoroutineScope(Dispatchers.IO).launch {
                    artPieceDao.insertArtPiece(artPiece)
                    withContext(Dispatchers.Main) {
                        localArtPieces.add(artPiece)
                        updateRecyclerView()
                        setupTagFilters()
                    }
                }
            },
            fragmentManager = supportFragmentManager
        )

        // --- 5) Button Listeners ---
        profileButton.setOnClickListener { openUserProfileFragment() }
        myArtButton.setOnClickListener { openUserArtFragment() }
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        uploadArtButton.setOnClickListener {
            imageUploadManager.showImageSourceSelectionDialog(resultLauncher)
        }
        clearPostsButton.setOnClickListener { clearAllLocalArtPieces() }

        // **Navigate to DownloadedImagesFragment** (must be in nav_graph.xml)
        downloadedImagesButton.setOnClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.downloadedImagesFragment)
        }

        // --- 6) Load Art Pieces from Room (local storage) ---
        loadArtPieces()

        // --- 7) Load User Info (via Firestore) ---
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userNameText.text = "Welcome to PhotoShare!"
                }
        }

        // --- 8) Navigation: Show/Hide UI based on Destination ---
        val navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
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

    /**
     * Loads art pieces from the local Room database.
     */
    private fun loadArtPieces() {
        CoroutineScope(Dispatchers.IO).launch {
            val artPiecesFromDb = artPieceDao.getAllArtPieces()
            localArtPieces.clear()
            localArtPieces.addAll(artPiecesFromDb)
            withContext(Dispatchers.Main) {
                updateRecyclerView()
                setupTagFilters()
            }
        }
    }

    /**
     * Called (for example, from ArtPieceFragment) to refresh the art list.
     *
     * @param forceImageReload If true, forces the adapter to reload images.
     */
    fun refreshArtPieces(forceImageReload: Boolean = false) {
        loadArtPieces()
        if (forceImageReload && ::artPieceAdapter.isInitialized) {
            artPieceAdapter.notifyDataSetChanged()
        }
    }

    // --- Fragment Navigation ---
    private fun openUserProfileFragment() {
        val navController = findNavController(R.id.nav_host_fragment)
        if (navController.currentDestination?.id == R.id.userProfileFragment) return
        hideRecyclerView()
        navController.navigate(R.id.userProfileFragment)
    }

    private fun openUserArtFragment() {
        val navController = findNavController(R.id.nav_host_fragment)
        if (navController.currentDestination?.id == R.id.myArtFragment) return
        hideRecyclerView()
        navController.navigate(R.id.myArtFragment)
    }

    fun openArtPieceFragment(artPiece: ArtPiece) {
        val navController = findNavController(R.id.nav_host_fragment)
        hideRecyclerView()
        val bundle = Bundle().apply { putParcelable("artPiece", artPiece) }
        navController.navigate(R.id.artPieceFragment, bundle)
    }

    // --- Show/Hide UI Helpers ---
    fun showMainButtons() {
        findViewById<Button>(R.id.uploadArtButton).visibility = View.VISIBLE
        findViewById<Button>(R.id.clearPostsButton).visibility = View.GONE
        findViewById<Button>(R.id.myArtButton).visibility = View.VISIBLE
        findViewById<Button>(R.id.toggleFiltersButton).visibility = View.VISIBLE
        findViewById<Button>(R.id.downloadedImagesButton).visibility = View.VISIBLE
    }

    fun hideMainButtons() {
        findViewById<Button>(R.id.uploadArtButton).visibility = View.GONE
        findViewById<Button>(R.id.clearPostsButton).visibility = View.GONE
        findViewById<Button>(R.id.myArtButton).visibility = View.GONE
        findViewById<Button>(R.id.toggleFiltersButton).visibility = View.GONE
        findViewById<Button>(R.id.downloadedImagesButton).visibility = View.GONE
    }

    fun showRecyclerView() {
        artPiecesRecyclerView.visibility = View.VISIBLE
        // Don't hide the navigation host fragment completely
        // Just make sure you're on the home fragment
        val navController = findNavController(R.id.nav_host_fragment)
        if (navController.currentDestination?.id != R.id.homeFragment) {
            navController.navigate(R.id.homeFragment)
        }
    }

    fun hideRecyclerView() {
        artPiecesRecyclerView.visibility = View.GONE
    }

    // --- Image Upload Result ---
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        imageUploadManager.handleImageResult(result.resultCode, result.data)
    }

    // --- Filtering and UI Helpers ---
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
            artPieceAdapter.updateList(sortArtPiecesByRating(localArtPieces))
        } else {
            val filteredList = localArtPieces.filter { piece ->
                // Current implementation only shows posts that have ANY selected tag
                // We should ensure it filters correctly
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
            // Skip empty tags
            if (tag.isBlank()) continue

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

    // --- Clearing and Updating Art Pieces ---
    private fun clearAllLocalArtPieces() {
        CoroutineScope(Dispatchers.IO).launch {
            localArtPieces.forEach { artPiece ->
                artPieceDao.deleteArtPiece(artPiece)
            }
            localArtPieces.clear()
            withContext(Dispatchers.Main) {
                updateRecyclerView()
                setupTagFilters()
            }
        }
    }

    fun updateArtPieceRating(artPiece: ArtPiece, isLike: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val likedBy = artPiece.likedBy.toMutableList()
        val dislikedBy = artPiece.dislikedBy.toMutableList()
        var likes = artPiece.likes
        var dislikes = artPiece.dislikes

        if (isLike) {
            if (likedBy.contains(currentUserId)) {
                likedBy.remove(currentUserId)
                likes--
            } else {
                likedBy.add(currentUserId)
                likes++
                if (dislikedBy.contains(currentUserId)) {
                    dislikedBy.remove(currentUserId)
                    dislikes--
                }
            }
        } else {
            if (dislikedBy.contains(currentUserId)) {
                dislikedBy.remove(currentUserId)
                dislikes--
            } else {
                dislikedBy.add(currentUserId)
                dislikes++
                if (likedBy.contains(currentUserId)) {
                    likedBy.remove(currentUserId)
                    likes--
                }
            }
        }

        val updatedArtPiece = artPiece.copy(
            likes = likes,
            dislikes = dislikes,
            likedBy = likedBy,
            dislikedBy = dislikedBy
        )

        CoroutineScope(Dispatchers.IO).launch {
            artPieceDao.updateArtPiece(updatedArtPiece)
        }

        val index = localArtPieces.indexOfFirst { it.artId == artPiece.artId }
        if (index != -1) {
            localArtPieces[index] = updatedArtPiece
        }
        artPieceAdapter.updateList(sortArtPiecesByRating(localArtPieces))
    }

    private fun sortArtPiecesByRating(pieces: List<ArtPiece>): List<ArtPiece> {
        return pieces.sortedByDescending { it.getRatingScore() }
    }

    private fun updateRecyclerView() {
        setupArtPieceAdapter()
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

    fun deleteArtPiece(artPiece: ArtPiece) {
        CoroutineScope(Dispatchers.IO).launch {
            artPieceDao.deleteArtPiece(artPiece)
            withContext(Dispatchers.Main) {
                localArtPieces.remove(artPiece)
                updateRecyclerView()
                setupTagFilters()
            }
        }
    }

    fun updateArtPiece(updatedArtPiece: ArtPiece) {
        CoroutineScope(Dispatchers.IO).launch {
            artPieceDao.updateArtPiece(updatedArtPiece)
            withContext(Dispatchers.Main) {
                refreshArtPieces()
            }
        }
    }
}
