package com.rs.photoshare.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rs.photoshare.models.Photo
import com.rs.photoshare.repository.PhotoRepository

class PhotoViewModel : ViewModel() {
    private val TAG = "PhotoViewModel"
    // Repository instance for fetching photos
    private val photoRepository = PhotoRepository()

    private val _photos = MutableLiveData<List<Photo>>()
    // LiveData for list of photos
    val photos: LiveData<List<Photo>> = _photos

    private val _currentPhoto = MutableLiveData<Photo>()
    // LiveData for current photo details
    val currentPhoto: LiveData<Photo> = _currentPhoto

    private val _isLoading = MutableLiveData(false)
    // LiveData for loading state
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    // LiveData for error messages
    val errorMessage: LiveData<String> = _errorMessage

    // Available avatar styles from repository
    val availableStyles = photoRepository.availableStyles

    private val _currentStyle = MutableLiveData(photoRepository.currentStyle)
    // LiveData for current avatar style
    val currentStyle: LiveData<String> = _currentStyle

    // Fetch photos from the repository
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

    // Fetch details of a specific photo
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

    // Fetch photos by a specific author
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

    // Set the avatar style and refresh photos
    fun setAvatarStyle(style: String) {
        if (style in availableStyles) {
            photoRepository.setAvatarStyle(style)
            _currentStyle.value = style
            fetchPhotos()
        }
    }
}
