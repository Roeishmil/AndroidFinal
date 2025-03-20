package com.rs.photoshare.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_images")
data class DownloadedImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID of the post we downloaded (optional, if you want to link them).
    val artId: String,

    // Local file path where the image is saved.
    val localPath: String,

    // We can store a title or other metadata so the user can identify the image.
    val title: String,

    // For sorting or showing the date/time downloaded
    val timestamp: Long
)
