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
    private lateinit var cloudinaryRepository: CloudinaryRepository

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

        val userNameText: TextView = findViewById(R.id.userNameText)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val uploadArtButton: Button = findViewById(R.id.uploadArtButton)
        val clearPostsButton: Button = findViewById(R.id.clearPostsButton)
        val profileButton: Button = findViewById(R.id.profileButton)
        val myArtButton: Button = findViewById(R.id.myArtButton)
        val downloadedImagesButton: Button = findViewById(R.id.downloadedImagesButton)
        cloudinaryRepository = CloudinaryRepository(this)

        filterContainer = findViewById(R.id.filterContainer)
        tagFilterLayout = findViewById(R.id.tagFilterLayout)
        applyFilterButton = findViewById(R.id.applyFilterButton)
        clearFilterButton = findViewById(R.id.clearFilterButton)
        toggleFiltersButton = findViewById(R.id.toggleFiltersButton)

        val database = AppDatabase.getDatabase(this)
        artPieceDao = database.artPieceDao()

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

        imageUploadManager = ImageUploadManager(
            context = this,
            auth = auth,
            progressBar = findViewById(R.id.progressBar),
            onArtPieceUploaded = { artPiece ->
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

        downloadedImagesButton.setOnClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.downloadedImagesFragment)
        }

        loadArtPieces()

        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    userNameText.text = "Welcome to PhotoShare!"
                }
        }

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
                    hideFilterContainer()
                }
            }
        }
    }

    private fun loadArtPieces() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, get all art pieces from the local database
                val localArtPiecesFromDb = artPieceDao.getAllArtPieces()
                val localArtPieceMap = localArtPiecesFromDb.associateBy { it.artId }

                // Then fetch all art pieces from Cloudinary
                val cloudinaryArtPieces = cloudinaryRepository.fetchAllArtPiecesAsync()

                // Merge both sources, preferring local versions which may have more data (like likedBy)
                val mergedArtPieces = mutableListOf<ArtPiece>()

                for (cloudinaryArtPiece in cloudinaryArtPieces) {
                    val localArtPiece = localArtPieceMap[cloudinaryArtPiece.artId]

                    if (localArtPiece != null) {
                        // We have this art piece locally, use local version but update imageUrl if needed
                        if (localArtPiece.imageUrl != cloudinaryArtPiece.imageUrl) {
                            val updatedArtPiece = localArtPiece.copy(imageUrl = cloudinaryArtPiece.imageUrl)
                            artPieceDao.updateArtPiece(updatedArtPiece)
                            mergedArtPieces.add(updatedArtPiece)
                        } else {
                            mergedArtPieces.add(localArtPiece)
                        }
                    } else {
                        // This is a new art piece from Cloudinary, add it to local DB
                        artPieceDao.insertArtPiece(cloudinaryArtPiece)
                        mergedArtPieces.add(cloudinaryArtPiece)
                    }
                }

                // Update our working list
                withContext(Dispatchers.Main) {
                    localArtPieces.clear()
                    localArtPieces.addAll(mergedArtPieces)
                    updateRecyclerView()
                    setupTagFilters()
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error syncing with Cloudinary: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Fallback to local content only
                    val localArtPiecesFromDb = artPieceDao.getAllArtPieces()
                    localArtPieces.clear()
                    localArtPieces.addAll(localArtPiecesFromDb)
                    updateRecyclerView()
                    setupTagFilters()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    fun refreshArtPieces(forceImageReload: Boolean = false) {
        loadArtPieces()
        if (forceImageReload && ::artPieceAdapter.isInitialized) {
            artPieceAdapter.notifyDataSetChanged()
        }
    }

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

    private fun hideFilterContainer() {
        filterContainer.visibility = View.GONE
        toggleFiltersButton.text = "Show Filters"
    }


    fun showMainButtons() {
        findViewById<Button>(R.id.uploadArtButton).visibility = View.VISIBLE
        findViewById<Button>(R.id.clearPostsButton).visibility = View.VISIBLE
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
        val navController = findNavController(R.id.nav_host_fragment)
        if (navController.currentDestination?.id != R.id.homeFragment) {
            navController.navigate(R.id.homeFragment)
        }
    }

    fun hideRecyclerView() {
        artPiecesRecyclerView.visibility = View.GONE
    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        imageUploadManager.handleImageResult(result.resultCode, result.data)
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

    private fun filterArtPieces() {
        if (selectedTags.isEmpty()) {
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
