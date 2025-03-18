package com.rs.photoshare.api

import com.rs.photoshare.models.Photo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PhotoApiService {
    @GET("v2/list")
    fun getPhotos(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<List<Photo>>

    @GET("id/{id}/info")
    fun getPhotoDetails(@Path("id") id: String): Call<Photo>

    @GET("v2/list")
    fun getPhotosByAuthor(
        @Query("author") author: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<List<Photo>>
}