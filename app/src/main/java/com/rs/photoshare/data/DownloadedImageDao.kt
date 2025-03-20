package com.rs.photoshare.data

import androidx.room.*
import com.rs.photoshare.models.DownloadedImage

@Dao
interface DownloadedImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedImage(downloadedImage: DownloadedImage)

    @Delete
    suspend fun deleteDownloadedImage(downloadedImage: DownloadedImage)

    @Query("SELECT * FROM downloaded_images ORDER BY timestamp DESC")
    suspend fun getAllDownloadedImages(): List<DownloadedImage>
}
