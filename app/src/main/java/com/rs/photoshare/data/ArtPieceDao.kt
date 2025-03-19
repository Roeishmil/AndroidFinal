package com.rs.photoshare.data

import androidx.room.*
import com.rs.photoshare.models.ArtPiece

@Dao
interface ArtPieceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtPiece(artPiece: ArtPiece)

    @Update
    suspend fun updateArtPiece(artPiece: ArtPiece)

    @Delete
    suspend fun deleteArtPiece(artPiece: ArtPiece)

    @Query("SELECT * FROM art_pieces WHERE creatorId = :userId ORDER BY timestamp DESC")
    suspend fun getUserArtPieces(userId: String): List<ArtPiece>

    @Query("SELECT * FROM art_pieces ORDER BY timestamp DESC")
    suspend fun getAllArtPieces(): List<ArtPiece>
}
