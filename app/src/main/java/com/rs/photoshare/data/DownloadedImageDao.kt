package com.rs.photoshare.data

import androidx.room.*
import com.rs.photoshare.models.DownloadedImage

// DAO for the DownloadedImage entity.
@Dao
interface DownloadedImageDao {

    // Inserts a DownloadedImage; replaces on conflict.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedImage(downloadedImage: DownloadedImage)

    // Deletes the specified DownloadedImage.
    @Delete
    suspend fun deleteDownloadedImage(downloadedImage: DownloadedImage)

    // Retrieves all DownloadedImages, ordered by newest timestamp.
    @Query("SELECT * FROM downloaded_images ORDER BY timestamp DESC")
    suspend fun getAllDownloadedImages(): List<DownloadedImage>
}
