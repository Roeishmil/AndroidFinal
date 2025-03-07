package com.rs.photoshare

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.rs.photoshare.models.ArtPiece
import com.squareup.picasso.Picasso
import java.io.File

class ArtPieceFragment : Fragment() {

    private lateinit var artPiece: ArtPiece

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        artPiece = requireArguments().getParcelable("artPiece")!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_art_piece, container, false)

        view.findViewById<TextView>(R.id.artPieceTitleView).text = artPiece.title
        view.findViewById<TextView>(R.id.artPieceDescriptionView).text = artPiece.description
        view.findViewById<TextView>(R.id.artPieceTagView).text = artPiece.tags.firstOrNull() ?: "No Tag"

        val imageView = view.findViewById<ImageView>(R.id.artPieceImageView)
        Picasso.get().load(File(artPiece.imageUrl)).into(imageView)

        view.findViewById<Button>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
            (activity as? MainActivity)?.showRecyclerView()
        }

        return view
    }


    companion object {
        fun newInstance(artPiece: ArtPiece) = ArtPieceFragment().apply {
            arguments = Bundle().apply {
                putParcelable("artPiece", artPiece)
            }
        }
    }
}
