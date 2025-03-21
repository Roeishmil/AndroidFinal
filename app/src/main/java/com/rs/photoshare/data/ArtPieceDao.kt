package com.rs.photoshare.data

import androidx.room.*
import com.rs.photoshare.models.ArtPiece

/**
 * Data Access Object for the ArtPiece entity.
 */
@Dao
interface ArtPieceDao {
     //Inserts an ArtPiece and replaces any existing entry on conflict.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtPiece(artPiece: ArtPiece)


    //Updates an existing ArtPiece.
    @Update
    suspend fun updateArtPiece(artPiece: ArtPiece)


    //Deletes the specified ArtPiece.
    @Delete
    suspend fun deleteArtPiece(artPiece: ArtPiece)


    //Retrieves ArtPieces created by a specific user, ordered by timestamp in descending order.
    @Query("SELECT * FROM art_pieces WHERE creatorId = :userId ORDER BY timestamp DESC")
    suspend fun getUserArtPieces(userId: String): List<ArtPiece>


     //Retrieves all ArtPieces, ordered by timestamp in descending order.
    @Query("SELECT * FROM art_pieces ORDER BY timestamp DESC")
    suspend fun getAllArtPieces(): List<ArtPiece>
}
