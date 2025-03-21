package com.rs.photoshare.repository

import android.util.Log
import com.rs.photoshare.api.RandomUserResponse
import com.rs.photoshare.api.RetrofitClient
import com.rs.photoshare.models.Photo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Repository for handling photo-related data operations.
class PhotoRepository {
    private val TAG = "PhotoRepository"
    private val photoApiService = RetrofitClient.photoApiService

    // Available avatar styles for the avatar feature.
    val availableStyles = listOf("avataaars", "bottts", "micah", "adventurer", "identicon")
    var currentStyle = "avataaars" // Default style

    // Set avatar style if valid.
    fun setAvatarStyle(style: String) {
        if (style in availableStyles) {
            currentStyle = style
        }
    }

    // Fetch a list of photos (mapped from random users).
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

    // Fetch details for a specific photo (simulated with a random user).
    fun getPhotoDetails(
        id: String,
        onSuccess: (Photo) -> Unit,
        onError: (String) -> Unit
    ) {
        photoApiService.getRandomUsers(results = 1).enqueue(object : Callback<RandomUserResponse> {
            override fun onResponse(call: Call<RandomUserResponse>, response: Response<RandomUserResponse>) {
                if (response.isSuccessful) {
                    val randomUserResponse = response.body()
                    if (randomUserResponse != null && randomUserResponse.results.isNotEmpty()) {
                        val user = randomUserResponse.results[0]
                        val photo = Photo(
                            id = id,
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

    // Fetch photos by a specific author (random user data, author overridden).
    fun getPhotosByAuthor(
        author: String,
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
                        val photos = randomUserResponse.results.map { user ->
                            Photo(
                                id = user.login.uuid,
                                author = author,
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
