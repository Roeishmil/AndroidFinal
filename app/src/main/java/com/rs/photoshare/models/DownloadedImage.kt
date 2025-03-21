package com.rs.photoshare.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity representing a downloaded image.
@Entity(tableName = "downloaded_images")
data class DownloadedImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID of the post we downloaded (optional for linking).
    val artId: String,

    // Local file path where the image is saved.
    val localPath: String,

    // Title or metadata to identify the image.
    val title: String,

    // Timestamp when the image was downloaded.
    val timestamp: Long
)
