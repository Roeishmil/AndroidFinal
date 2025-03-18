import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rs.photoshare.R
import com.rs.photoshare.models.ArtPiece
import com.squareup.picasso.Picasso
import java.io.File

class ArtPieceAdapter(
    private var artPieces: List<ArtPiece>,
    private val onItemClick: (ArtPiece) -> Unit
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

        fun bind(artPiece: ArtPiece, onItemClick: (ArtPiece) -> Unit) {
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

            itemView.setOnClickListener { onItemClick(artPiece) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtPieceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.art_piece_item, parent, false)
        return ArtPieceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtPieceViewHolder, position: Int) {
        holder.bind(artPieces[position], onItemClick)
    }

    override fun getItemCount(): Int = artPieces.size
}