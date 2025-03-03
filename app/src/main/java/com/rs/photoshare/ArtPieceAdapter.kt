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

class ArtPieceAdapter(private val artPieces: List<ArtPiece>) : RecyclerView.Adapter<ArtPieceAdapter.ArtPieceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtPieceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.art_piece_item, parent, false)
        return ArtPieceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtPieceViewHolder, position: Int) {
        holder.bind(artPieces[position])
    }

    override fun getItemCount(): Int = artPieces.size

    class ArtPieceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.artPieceTitle)
        private val description: TextView = itemView.findViewById(R.id.artPieceDescription)
        private val imageView: ImageView = itemView.findViewById(R.id.artPieceImage)

        fun bind(artPiece: ArtPiece) {
            title.text = artPiece.title
            description.text = artPiece.description

            val file = File(artPiece.imageUrl)
            if (file.exists()) {
                Picasso.get().load(file).into(imageView)
            } else {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }
}
