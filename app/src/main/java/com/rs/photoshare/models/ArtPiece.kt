package com.rs.photoshare.models

import android.os.Parcel
import android.os.Parcelable

data class ArtPiece(
    val artId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val thumbnailPath: String? = null,
    val tags: List<String> = emptyList(),
    val creatorId: String = "",
    val timestamp: Long = 0L
) : Parcelable {
    constructor(parcel: Parcel) : this(
        artId = parcel.readString() ?: "",
        title = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        imageUrl = parcel.readString() ?: "",
        thumbnailPath = parcel.readString(),
        tags = parcel.createStringArrayList() ?: emptyList(),
        creatorId = parcel.readString() ?: "",
        timestamp = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(artId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeString(thumbnailPath)
        parcel.writeStringList(tags)
        parcel.writeString(creatorId)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ArtPiece> {
        override fun createFromParcel(parcel: Parcel): ArtPiece {
            return ArtPiece(parcel)
        }

        override fun newArray(size: Int): Array<ArtPiece?> {
            return arrayOfNulls(size)
        }
    }
}