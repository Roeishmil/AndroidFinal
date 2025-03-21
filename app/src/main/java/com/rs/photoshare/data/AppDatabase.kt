package com.rs.photoshare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rs.photoshare.models.ArtPiece
import com.rs.photoshare.models.DownloadedImage

/**
 * Main Room database for ArtPiece and DownloadedImage entities.
 */
@Database(
    entities = [ArtPiece::class, DownloadedImage::class],
    version = 2,  // bumped version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun artPieceDao(): ArtPieceDao
    abstract fun downloadedImageDao(): DownloadedImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns a singleton instance of AppDatabase.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Use fallbackToDestructiveMigration() for now.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "art_piece_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
