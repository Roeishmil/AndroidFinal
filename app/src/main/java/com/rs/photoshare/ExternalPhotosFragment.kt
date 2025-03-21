package com.rs.photoshare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rs.photoshare.models.Photo
import com.rs.photoshare.models.PhotoViewModel

// Fragment that displays a grid of external photos using a ViewModel and RecyclerView
class ExternalPhotosFragment : Fragment() {

    private lateinit var photoViewModel: PhotoViewModel // ViewModel to manage photo data
    private lateinit var recyclerView: RecyclerView // RecyclerView to display photos
    private lateinit var progressBar: ProgressBar // Progress bar to show loading state
    private lateinit var photoAdapter: ExternalPhotoAdapter // Adapter for the photo list

    // Inflate the fragment layout and initialize views
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_external_photos, container, false)

        recyclerView = view.findViewById(R.id.externalPhotosRecyclerView) // Find RecyclerView
        progressBar = view.findViewById(R.id.progressBar) // Find ProgressBar

        setupRecyclerView() // Set up RecyclerView with adapter and layout

        return view
    }

    // Observe LiveData from the ViewModel and handle UI updates
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoViewModel = ViewModelProvider(this)[PhotoViewModel::class.java] // Get ViewModel

        // Observe photo list updates
        photoViewModel.photos.observe(viewLifecycleOwner) { photos ->
            photoAdapter.updatePhotos(photos)
        }

        // Observe loading state to show/hide progress bar
        photoViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages and show them as Toasts
        photoViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
        }

        // Fetch the first page of photos when the fragment is created
        photoViewModel.fetchPhotos()
    }

    // Set up the RecyclerView with a GridLayoutManager and adapter
    private fun setupRecyclerView() {
        photoAdapter = ExternalPhotoAdapter(emptyList()) { photo ->
            // Handle photo click - you can add navigation to photo details here
            Toast.makeText(requireContext(), "Selected photo by ${photo.author}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2) // Grid with 2 columns
            adapter = photoAdapter

            // Add scroll listener for pagination (load more when reaching the bottom)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (totalItemCount <= (lastVisibleItem + 2)) {
                        // Load more photos when near the end of the list
                        val nextPage = (totalItemCount / 20) + 1
                        photoViewModel.fetchPhotos(page = nextPage)
                    }
                }
            })
        }
    }
}
