package com.rs.photoshare

import ArtPieceAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
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
        val view = inflater.inflate(R.layout.fragment_my_art, container, false)
        recyclerView = view.findViewById(R.id.userArtRecyclerView)
        val backButton: Button = view.findViewById(R.id.backButton)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadUserArtPieces()

        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        return view
    }

    private fun loadUserArtPieces() {
        val currentUserId = auth.currentUser?.uid ?: return
        userArtPieces.clear()

        val files = requireContext().filesDir.listFiles { file -> file.extension == "json" } ?: emptyArray()
        for (metadataFile in files) {
            val artPiece = readArtPieceFromJson(metadataFile)
            if (artPiece != null && artPiece.creatorId == currentUserId) {
                userArtPieces.add(artPiece)
            }
        }

        artPieceAdapter = ArtPieceAdapter(userArtPieces) { artPiece ->
            // We'll ask MainActivity to navigate to the ArtPieceFragment
            // Or navigate directly using the NavController if you like:
            (activity as? MainActivity)?.openArtPieceFragment(artPiece)
        }
        recyclerView.adapter = artPieceAdapter
    }

    private fun readArtPieceFromJson(file: File): ArtPiece? {
        return try {
            val gson = com.google.gson.Gson()
            gson.fromJson(file.readText(), ArtPiece::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
