import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rs.photoshare.R
import com.rs.photoshare.managers.AuthManager
import com.rs.photoshare.models.ArtPiece
import com.squareup.picasso.Picasso
import java.io.File

class ArtPieceAdapter(
    private var artPieces: List<ArtPiece>,
    private val onItemClick: (ArtPiece) -> Unit,
    private val onLikeClick: (ArtPiece) -> Unit,
    private val onDislikeClick: (ArtPiece) -> Unit
) : RecyclerView.Adapter<ArtPieceAdapter.ArtPieceViewHolder>() {

    fun updateList(newList: List<ArtPiece>) {
        artPieces = newList
        notifyDataSetChanged()
    }

    class ArtPieceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.artPieceTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.artPieceDescription)
        private val imageView: ImageView = itemView.findViewById(R.id.artPieceImage)
        private val tagTextView: TextView = itemView.findViewById(R.id.artPieceTag)
        private val likeButton: ImageButton = itemView.findViewById(R.id.likeButton)
        private val dislikeButton: ImageButton = itemView.findViewById(R.id.dislikeButton)
        private val likeCount: TextView = itemView.findViewById(R.id.likeCount)
        private val dislikeCount: TextView = itemView.findViewById(R.id.dislikeCount)

        fun bind(
            artPiece: ArtPiece,
            onItemClick: (ArtPiece) -> Unit,
            onLikeClick: (ArtPiece) -> Unit,
            onDislikeClick: (ArtPiece) -> Unit
        ) {
            titleTextView.text = artPiece.title
            descriptionTextView.text = artPiece.description

            // Display multiple tags if present
            val tagsText = if (artPiece.tags.size > 1) {
                artPiece.tags.joinToString(", ")
            } else {
                artPiece.tags.firstOrNull() ?: "No Tag"
            }
            tagTextView.text = tagsText

            // Load image from Cloudinary URL or local file
            if (artPiece.imageUrl.startsWith("http")) {
                // It's a Cloudinary URL
                Picasso.get().load(artPiece.imageUrl).into(imageView)
            } else {
                // It's a local file path from the old system
                Picasso.get().load(File(artPiece.imageUrl)).into(imageView)
            }

            // Set like/dislike counts
            likeCount.text = artPiece.likes.toString()
            dislikeCount.text = artPiece.dislikes.toString()

            // Visual indication of user's rating (if you add current user's ID to likedBy/dislikedBy)
            val currentUserId = AuthManager().getCurrentUserId()
            if (artPiece.likedBy.contains(currentUserId)) {
                likeButton.setImageResource(R.drawable.baseline_thumb_up_24) // Replace with thumbs up filled
                dislikeButton.setImageResource(R.drawable.baseline_thumb_down_24) // Default dislike icon
            } else if (artPiece.dislikedBy.contains(currentUserId)) {
                likeButton.setImageResource(R.drawable.baseline_thumb_up_off_alt_24) // Default like icon
                dislikeButton.setImageResource(R.drawable.baseline_thumb_down_off_alt_24) // Replace with thumbs down filled
            } else {
                likeButton.setImageResource(R.drawable.baseline_thumb_up_24) // Default like icon
                dislikeButton.setImageResource(R.drawable.baseline_thumb_down_24) // Default dislike icon
            }

            // Set up click listeners
            itemView.setOnClickListener { onItemClick(artPiece) }
            likeButton.setOnClickListener { onLikeClick(artPiece) }
            dislikeButton.setOnClickListener { onDislikeClick(artPiece) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtPieceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.art_piece_item, parent, false)
        return ArtPieceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtPieceViewHolder, position: Int) {
        holder.bind(artPieces[position], onItemClick, onLikeClick, onDislikeClick)
    }

    override fun getItemCount(): Int = artPieces.size
}