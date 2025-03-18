package com.rs.photoshare.repository

import android.util.Log
import com.rs.photoshare.api.RetrofitClient
import com.rs.photoshare.models.Photo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PhotoRepository {
    private val TAG = "PhotoRepository"
    private val photoApiService = RetrofitClient.photoApiService

    fun getPhotos(
        page: Int = 1,
        limit: Int = 20,
        onSuccess: (List<Photo>) -> Unit,
        onError: (String) -> Unit
    ) {
        photoApiService.getPhotos(page, limit).enqueue(object : Callback<List<Photo>> {
            override fun onResponse(call: Call<List<Photo>>, response: Response<List<Photo>>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        onSuccess(it)
                    } ?: onError("Response body is null")
                } else {
                    onError("Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<Photo>>, t: Throwable) {
                Log.e(TAG, "API call failed", t)
                onError("Network error: ${t.message}")
            }
        })
    }

    fun getPhotoDetails(
        id: String,
        onSuccess: (Photo) -> Unit,
        onError: (String) -> Unit
    ) {
        photoApiService.getPhotoDetails(id).enqueue(object : Callback<Photo> {
            override fun onResponse(call: Call<Photo>, response: Response<Photo>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        onSuccess(it)
                    } ?: onError("Response body is null")
                } else {
                    onError("Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Photo>, t: Throwable) {
                Log.e(TAG, "API call failed", t)
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
        photoApiService.getPhotosByAuthor(author, page, limit).enqueue(object : Callback<List<Photo>> {
            override fun onResponse(call: Call<List<Photo>>, response: Response<List<Photo>>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        onSuccess(it)
                    } ?: onError("Response body is null")
                } else {
                    onError("Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<Photo>>, t: Throwable) {
                Log.e(TAG, "API call failed", t)
                onError("Network error: ${t.message}")
            }
        })
    }
}