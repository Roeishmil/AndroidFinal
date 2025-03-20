package com.rs.photoshare.api

import com.rs.photoshare.models.Photo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PhotoApiService {
    // Random User API call
    @GET("api/")
    fun getRandomUsers(
        @Query("results") results: Int = 20,
        @Query("page") page: Int = 1
    ): Call<RandomUserResponse>

    // Keep these for compatibility
    @GET("v2/list")
    fun getPhotos(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<List<Photo>>

    @GET("id/{id}")
    fun getPhotoDetails(
        @Path("id") id: String
    ): Call<Photo>

    @GET("list")
    fun getPhotosByAuthor(
        @Query("author") author: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<List<Photo>>
}

// Add these data classes for Random User API
data class RandomUserResponse(
    val results: List<RandomUser>,
    val info: RandomUserInfo
)

data class RandomUserInfo(
    val seed: String,
    val results: Int,
    val page: Int,
    val version: String
)

data class RandomUser(
    val login: RandomUserLogin,
    val name: RandomUserName,
    val picture: RandomUserPicture
)

data class RandomUserLogin(
    val uuid: String
)

data class RandomUserName(
    val first: String,
    val last: String
)

data class RandomUserPicture(
    val large: String,
    val medium: String,
    val thumbnail: String
)