package com.rs.photoshare.models

data class User(
    val userId: String = "", // Unique identifier for the user
    val name: String = "", // Name of the user
    val email: String = "", // Email address of the user
    val profilePictureUrl: String = "", // URL or path to the user's profile picture
    val uploadedArtPiece: List<String> = emptyList() // List of design IDs uploaded by the user
)
