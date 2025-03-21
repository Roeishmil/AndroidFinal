package com.rs.photoshare.api

import com.rs.photoshare.models.Photo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// API service for photo sharing and random user endpoints.
interface PhotoApiService {

    // Retrieves random users (default 20 per page).
    @GET("api/")
    fun getRandomUsers(
        @Query("results") results: Int = 20,
        @Query("page") page: Int = 1
    ): Call<RandomUserResponse>

    // Retrieves a list of photos (for compatibility).
    @GET("v2/list")
    fun getPhotos(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<List<Photo>>

    // Retrieves details for a specific photo.
    @GET("id/{id}")
    fun getPhotoDetails(
        @Path("id") id: String
    ): Call<Photo>

    // Retrieves photos by a specific author.
    @GET("list")
    fun getPhotosByAuthor(
        @Query("author") author: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<List<Photo>>
}

// Response data for the Random User API.
data class RandomUserResponse(
    val results: List<RandomUser>,
    val info: RandomUserInfo
)

// Metadata for random user response.
data class RandomUserInfo(
    val seed: String,
    val results: Int,
    val page: Int,
    val version: String
)

// Details of a random user.
data class RandomUser(
    val login: RandomUserLogin,
    val name: RandomUserName,
    val picture: RandomUserPicture
)

// Random user login information.
data class RandomUserLogin(
    val uuid: String
)

// Random user name.
data class RandomUserName(
    val first: String,
    val last: String
)

// Random user picture URLs.
data class RandomUserPicture(
    val large: String,
    val medium: String,
    val thumbnail: String
)
