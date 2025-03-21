package com.rs.photoshare

import ArtPieceAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.rs.photoshare.models.ArtPiece
import java.io.File

class MyArtFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var artPieceAdapter: ArtPieceAdapter
    private val userArtPieces = mutableListOf<ArtPiece>()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_art, container, false)

        // Initialize RecyclerView and back button
        recyclerView = view.findViewById(R.id.userArtRecyclerView)
        val backButton: Button = view.findViewById(R.id.backButton)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Load user's own art pieces from local storage
        loadUserArtPieces()

        // Navigate back when back button is clicked
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        return view
    }

    private fun loadUserArtPieces() {
        // Get the current user ID
        val currentUserId = auth.currentUser?.uid ?: return
        userArtPieces.clear()

        // Read all JSON files from local storage
        val files = requireContext().filesDir.listFiles { file -> file.extension == "json" } ?: emptyArray()
        for (metadataFile in files) {
            val artPiece = readArtPieceFromJson(metadataFile)
            // Only add art pieces created by the current user
            if (artPiece != null && artPiece.creatorId == currentUserId) {
                userArtPieces.add(artPiece)
            }
        }

        // Set up the adapter with click handlers
        artPieceAdapter = ArtPieceAdapter(
            userArtPieces,
            onItemClick = { artPiece ->
                // Open detailed view of selected art piece
                (activity as? MainActivity)?.openArtPieceFragment(artPiece)
            },
            onLikeClick = { artPiece ->
                // Handle like action
                (activity as? MainActivity)?.updateArtPieceRating(artPiece, true)
            },
            onDislikeClick = { artPiece ->
                // Handle dislike action
                (activity as? MainActivity)?.updateArtPieceRating(artPiece, false)
            }
        )
        recyclerView.adapter = artPieceAdapter
    }

    private fun readArtPieceFromJson(file: File): ArtPiece? {
        // Parse JSON file into an ArtPiece object
        return try {
            val gson = com.google.gson.Gson()
            gson.fromJson(file.readText(), ArtPiece::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
