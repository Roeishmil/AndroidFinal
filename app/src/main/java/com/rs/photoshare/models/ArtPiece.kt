package com.rs.photoshare.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents an art piece stored in Room DB; implements Parcelable.
@Entity(tableName = "art_pieces")
data class ArtPiece(
    @PrimaryKey val artId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val thumbnailPath: String? = null,
    val tags: List<String> = emptyList(),
    val creatorId: String = "",
    val timestamp: Long = 0L,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val dislikedBy: List<String> = emptyList()
) : Parcelable {
    // Recreates ArtPiece from a Parcel.
    constructor(parcel: Parcel) : this(
        artId = parcel.readString() ?: "",
        title = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        imageUrl = parcel.readString() ?: "",
        thumbnailPath = parcel.readString(),
        tags = parcel.createStringArrayList() ?: emptyList(),
        creatorId = parcel.readString() ?: "",
        timestamp = parcel.readLong(),
        likes = parcel.readInt(),
        dislikes = parcel.readInt(),
        likedBy = parcel.createStringArrayList() ?: emptyList(),
        dislikedBy = parcel.createStringArrayList() ?: emptyList()
    )

    // Writes ArtPiece properties to a Parcel.
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(artId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeString(thumbnailPath)
        parcel.writeStringList(tags)
        parcel.writeString(creatorId)
        parcel.writeLong(timestamp)
        parcel.writeInt(likes)
        parcel.writeInt(dislikes)
        parcel.writeStringList(likedBy)
        parcel.writeStringList(dislikedBy)
    }

    // No special contents.
    override fun describeContents(): Int = 0

    // Parcelable.Creator for creating ArtPiece instances.
    companion object CREATOR : Parcelable.Creator<ArtPiece> {
        override fun createFromParcel(parcel: Parcel): ArtPiece {
            return ArtPiece(parcel)
        }

        override fun newArray(size: Int): Array<ArtPiece?> {
            return arrayOfNulls(size)
        }
    }

    // Returns the rating score (likes minus dislikes).
    fun getRatingScore(): Int {
        return likes - dislikes
    }
}
