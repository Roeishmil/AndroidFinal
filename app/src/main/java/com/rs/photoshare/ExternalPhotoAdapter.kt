package com.rs.photoshare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rs.photoshare.models.Photo
import com.squareup.picasso.Picasso

// RecyclerView adapter for displaying external photos.
class ExternalPhotoAdapter(
    private var photos: List<Photo>, // List of Photo objects to be displayed.
    private val onPhotoClick: (Photo) -> Unit // Callback function triggered when a photo is clicked.
) : RecyclerView.Adapter<ExternalPhotoAdapter.PhotoViewHolder>() {

    // Updates the adapter with a new list of photos.
    fun updatePhotos(newPhotos: List<Photo>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    // Creates a new ViewHolder for an item in the RecyclerView.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_photo, parent, false)
        return PhotoViewHolder(view)
    }

    // Binds data to the ViewHolder at a given position.
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], onPhotoClick)
    }

    // Returns the number of items in the RecyclerView.
    override fun getItemCount(): Int = photos.size

    // ViewHolder class for managing individual photo items.
    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView) // ImageView for displaying the photo.
        private val authorTextView: TextView = itemView.findViewById(R.id.authorTextView) // TextView for displaying author name.

        // Binds a Photo object to the ViewHolder.
        fun bind(photo: Photo, onPhotoClick: (Photo) -> Unit) {
            authorTextView.text = photo.author // Set author name.

            // Load the photo using Picasso.
            Picasso.get()
                .load(photo.download_url) // Load image from URL.
                .placeholder(R.drawable.placeholder_image) // Show placeholder while loading.
                .error(R.drawable.error_image) // Show error image if loading fails.
                .into(photoImageView) // Set the image into the ImageView.

            // Set click listener on the item.
            itemView.setOnClickListener { onPhotoClick(photo) }
        }
    }
}
