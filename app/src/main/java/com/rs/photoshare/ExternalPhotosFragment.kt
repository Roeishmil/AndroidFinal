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

class ExternalPhotosFragment : Fragment() {

    private lateinit var photoViewModel: PhotoViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var photoAdapter: ExternalPhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_external_photos, container, false)

        recyclerView = view.findViewById(R.id.externalPhotosRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)

        setupRecyclerView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoViewModel = ViewModelProvider(this)[PhotoViewModel::class.java]

        photoViewModel.photos.observe(viewLifecycleOwner) { photos ->
            photoAdapter.updatePhotos(photos)
        }

        photoViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        photoViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
        }

        // Fetch photos when fragment is created
        photoViewModel.fetchPhotos()
    }

    private fun setupRecyclerView() {
        photoAdapter = ExternalPhotoAdapter(emptyList()) { photo ->
            // Handle photo click - you can add navigation to photo details here
            Toast.makeText(requireContext(), "Selected photo by ${photo.author}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = photoAdapter

            // Add pagination - load more photos when scrolling to the bottom
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (totalItemCount <= (lastVisibleItem + 2)) {
                        // Load more photos when near the end
                        val nextPage = (totalItemCount / 20) + 1
                        photoViewModel.fetchPhotos(page = nextPage)
                    }
                }
            })
        }
    }
}