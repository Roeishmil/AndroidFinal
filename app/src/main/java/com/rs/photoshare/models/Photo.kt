package com.rs.photoshare.models

// Data class representing a photo.
data class Photo(
    val id: String, // Unique identifier for the photo.
    val author: String, // Author of the photo.
    val width: Int, // Width of the photo in pixels.
    val height: Int, // Height of the photo in pixels.
    val url: String, // URL to the photo page.
    val download_url: String // URL to download the photo.
)
