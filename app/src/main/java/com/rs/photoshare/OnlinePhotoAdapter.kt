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
    private val photos: List<Photo>, // List of photos to display
    private val onPhotoSelected: (Photo) -> Unit // Callback when a photo is clicked
) : RecyclerView.Adapter<OnlinePhotoAdapter.PhotoViewHolder>() {

    // ViewHolder class to hold item views
    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.photoImageView) // Image view to show photo
        private val authorTextView: TextView = itemView.findViewById(R.id.photoAuthorTextView) // Text view for author name

        fun bind(photo: Photo, onPhotoSelected: (Photo) -> Unit) {
            // Load the image from URL using Picasso
            Picasso.get()
                .load(photo.url)
                .placeholder(R.drawable.placeholder_image) // Placeholder image during loading
                .error(R.drawable.error_image) // Fallback image on error
                .into(imageView)

            // Set the author's name
            authorTextView.text = photo.author

            // Handle item click
            itemView.setOnClickListener { onPhotoSelected(photo) }
        }
    }

    // Inflate layout and create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_online_photo, parent, false
        )
        return PhotoViewHolder(view)
    }

    // Bind data to the ViewHolder
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], onPhotoSelected)
    }

    // Return total number of items
    override fun getItemCount(): Int = photos.size
}
