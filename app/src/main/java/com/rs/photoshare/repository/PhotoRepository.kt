package com.rs.photoshare.repository

import android.util.Log
import com.rs.photoshare.api.RandomUserResponse
import com.rs.photoshare.api.RetrofitClient
import com.rs.photoshare.models.Photo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PhotoRepository {
    private val TAG = "PhotoRepository"
    private val photoApiService = RetrofitClient.photoApiService

    // Add available styles for avatars (for the avatar style feature)
    val availableStyles = listOf("avataaars", "bottts", "micah", "adventurer", "identicon")
    var currentStyle = "avataaars" // Default style

    fun setAvatarStyle(style: String) {
        if (style in availableStyles) {
            currentStyle = style
        }
    }

    fun getPhotos(
        page: Int = 1,
        limit: Int = 20,
        onSuccess: (List<Photo>) -> Unit,
        onError: (String) -> Unit
    ) {
        photoApiService.getRandomUsers(results = limit, page = page).enqueue(object : Callback<RandomUserResponse> {
            override fun onResponse(call: Call<RandomUserResponse>, response: Response<RandomUserResponse>) {
                if (response.isSuccessful) {
                    val randomUserResponse = response.body()
                    if (randomUserResponse != null) {
                        // Convert RandomUser objects to Photo objects
                        val photos = randomUserResponse.results.map { user ->
                            Photo(
                                id = user.login.uuid,
                                author = "${user.name.first} ${user.name.last}",
                                width = 128,
                                height = 128,
                                url = user.picture.medium,
                                download_url = user.picture.large
                            )
                        }
                        onSuccess(photos)
                    } else {
                        onError("Empty response body")
                    }
                } else {
                    onError("Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RandomUserResponse>, t: Throwable) {
                onError("Network error: ${t.message}")
            }
        })
    }

    fun getPhotoDetails(
        id: String,
        onSuccess: (Photo) -> Unit,
        onError: (String) -> Unit
    ) {
        // For a single photo with ID, we'll need to make a request to get a new random user
        // and pretend it's the one with the requested ID
        photoApiService.getRandomUsers(results = 1).enqueue(object : Callback<RandomUserResponse> {
            override fun onResponse(call: Call<RandomUserResponse>, response: Response<RandomUserResponse>) {
                if (response.isSuccessful) {
                    val randomUserResponse = response.body()
                    if (randomUserResponse != null && randomUserResponse.results.isNotEmpty()) {
                        val user = randomUserResponse.results[0]
                        val photo = Photo(
                            id = id, // Use the requested ID
                            author = "${user.name.first} ${user.name.last}",
                            width = 300,
                            height = 300,
                            url = user.picture.medium,
                            download_url = user.picture.large
                        )
                        onSuccess(photo)
                    } else {
                        onError("Empty response body")
                    }
                } else {
                    onError("Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RandomUserResponse>, t: Throwable) {
                onError("Network error: ${t.message}")
            }
        })
    }

    fun getPhotosByAuthor(
        author: String,
        page: Int = 1,
        limit: Int = 20,
        onSuccess: (List<Photo>) -> Unit,
        onError: (String) -> Unit
    ) {
        // Just get random photos and assign the author
        photoApiService.getRandomUsers(results = limit, page = page).enqueue(object : Callback<RandomUserResponse> {
            override fun onResponse(call: Call<RandomUserResponse>, response: Response<RandomUserResponse>) {
                if (response.isSuccessful) {
                    val randomUserResponse = response.body()
                    if (randomUserResponse != null) {
                        // Convert RandomUser objects to Photo objects with specified author
                        val photos = randomUserResponse.results.map { user ->
                            Photo(
                                id = user.login.uuid,
                                author = author, // Use the requested author
                                width = 128,
                                height = 128,
                                url = user.picture.medium,
                                download_url = user.picture.large
                            )
                        }
                        onSuccess(photos)
                    } else {
                        onError("Empty response body")
                    }
                } else {
                    onError("Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RandomUserResponse>, t: Throwable) {
                onError("Network error: ${t.message}")
            }
        })
    }
}