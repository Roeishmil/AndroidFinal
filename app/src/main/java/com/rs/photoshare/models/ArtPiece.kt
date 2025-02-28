package com.rs.photoshare.models

data class ArtPiece(
    val artId: String = "", // Unique identifier for the art piece
    val title: String = "",     // Title of the art piece
    val description: String = "", // Description of the art piece
    val imageUrl: String = "", // URL or path to the image associated with the art piece
    val tags: List<String> = emptyList(), // List of tags associated with the art piece
    val creatorId: String = "", // ID of the user who created the art piece
    val timestamp: Long = 0L // Timestamp indicating when the art piece was created
)