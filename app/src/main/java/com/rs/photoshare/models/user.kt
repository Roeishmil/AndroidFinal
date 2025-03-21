package com.rs.photoshare.models

// Data class representing a user.
data class User(
    val userId: String = "", // Unique identifier for the user.
    val name: String = "", // User's name.
    val email: String = "", // User's email address.
    val profilePictureUrl: String = "", // URL or path to the user's profile picture.
    val uploadedArtPiece: List<String> = emptyList() // List of art piece IDs uploaded by the user.
)
