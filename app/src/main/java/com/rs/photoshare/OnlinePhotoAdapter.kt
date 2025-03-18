package com.rs.photoshare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rs.photoshare.models.Photo
import com.squareup.picasso.Picasso

class OnlinePhotoAdapter(
    private val photos: List<Photo>,
    private val onPhotoSelected: (Photo) -> Unit
) : RecyclerView.Adapter<OnlinePhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val authorTextView: TextView = itemView.findViewById(R.id.photoAuthorTextView)

        fun bind(photo: Photo, onPhotoSelected: (Photo) -> Unit) {
            // Load the thumbnail version of the image
            val thumbnailUrl = photo.download_url.replace(
                Regex("(\\d+)/(\\d+)$"),
                "300/300"
            )

            Picasso.get()
                .load(thumbnailUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView)

            authorTextView.text = photo.author

            itemView.setOnClickListener { onPhotoSelected(photo) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_online_photo, parent, false
        )
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], onPhotoSelected)
    }

    override fun getItemCount(): Int = photos.size
}