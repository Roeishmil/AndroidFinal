package com.rs.photoshare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rs.photoshare.models.Photo
import com.squareup.picasso.Picasso

class ExternalPhotoAdapter(
    private var photos: List<Photo>,
    private val onPhotoClick: (Photo) -> Unit
) : RecyclerView.Adapter<ExternalPhotoAdapter.PhotoViewHolder>() {

    fun updatePhotos(newPhotos: List<Photo>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], onPhotoClick)
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val authorTextView: TextView = itemView.findViewById(R.id.authorTextView)

        fun bind(photo: Photo, onPhotoClick: (Photo) -> Unit) {
            authorTextView.text = photo.author

            // Load the photo using Picasso
            Picasso.get()
                .load(photo.download_url)
                .placeholder(R.drawable.placeholder_image) // Create a placeholder drawable
                .error(R.drawable.error_image) // Create an error drawable
                .into(photoImageView)

            itemView.setOnClickListener { onPhotoClick(photo) }
        }
    }
}