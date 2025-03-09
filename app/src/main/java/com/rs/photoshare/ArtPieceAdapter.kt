import android.net.Uri
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
    private val artPieces: List<ArtPiece>,
    private val onItemClick: (ArtPiece) -> Unit
) : RecyclerView.Adapter<ArtPieceAdapter.ArtPieceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtPieceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.art_piece_item, parent, false)
        return ArtPieceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtPieceViewHolder, position: Int) {
        holder.bind(artPieces[position], onItemClick)
    }

    class ArtPieceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.artPieceTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.artPieceDescription)
        private val imageView: ImageView = itemView.findViewById(R.id.artPieceImage)
        private val tagTextView: TextView = itemView.findViewById(R.id.artPieceTag)

        fun bind(artPiece: ArtPiece, onItemClick: (ArtPiece) -> Unit) {
            titleTextView.text = artPiece.title
            descriptionTextView.text = artPiece.description
            tagTextView.text = artPiece.tags.firstOrNull() ?: "No Tag"

            // Force Picasso to reload the image
            Picasso.get().invalidate(File(artPiece.imageUrl))
            Picasso.get().load(File(artPiece.imageUrl)).into(imageView)

            itemView.setOnClickListener { onItemClick(artPiece) }
        }
    }

    override fun getItemCount(): Int = artPieces.size

}
