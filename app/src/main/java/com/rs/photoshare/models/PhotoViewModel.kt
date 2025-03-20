package com.rs.photoshare.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rs.photoshare.models.Photo
import com.rs.photoshare.repository.PhotoRepository

class PhotoViewModel : ViewModel() {
    private val TAG = "PhotoViewModel"
    private val photoRepository = PhotoRepository()

    private val _photos = MutableLiveData<List<Photo>>()
    val photos: LiveData<List<Photo>> = _photos

    private val _currentPhoto = MutableLiveData<Photo>()
    val currentPhoto: LiveData<Photo> = _currentPhoto

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Add avatar style-related properties
    val availableStyles = photoRepository.availableStyles

    private val _currentStyle = MutableLiveData(photoRepository.currentStyle)
    val currentStyle: LiveData<String> = _currentStyle

    fun fetchPhotos(page: Int = 1, limit: Int = 20) {
        _isLoading.value = true
        photoRepository.getPhotos(
            page = page,
            limit = limit,
            onSuccess = { photoList ->
                _photos.value = photoList
                _isLoading.value = false
                Log.d(TAG, "Fetched ${photoList.size} photos")
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
                Log.e(TAG, "Error fetching photos: $error")
            }
        )
    }

    fun fetchPhotoDetails(id: String) {
        _isLoading.value = true
        photoRepository.getPhotoDetails(
            id = id,
            onSuccess = { photo ->
                _currentPhoto.value = photo
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    fun fetchPhotosByAuthor(author: String, page: Int = 1, limit: Int = 20) {
        _isLoading.value = true
        photoRepository.getPhotosByAuthor(
            author = author,
            page = page,
            limit = limit,
            onSuccess = { photoList ->
                _photos.value = photoList
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    // New method to change avatar style
    fun setAvatarStyle(style: String) {
        if (style in availableStyles) {
            photoRepository.setAvatarStyle(style)
            _currentStyle.value = style
            // Refresh photos to show new style
            fetchPhotos()
        }
    }
}