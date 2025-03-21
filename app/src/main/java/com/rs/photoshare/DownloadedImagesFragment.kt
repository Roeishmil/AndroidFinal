package com.rs.photoshare

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rs.photoshare.data.AppDatabase
import com.rs.photoshare.models.DownloadedImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadedImagesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView // RecyclerView for displaying downloaded images
    private lateinit var adapter: DownloadedImagesAdapter // Adapter for managing list items
    private val downloadedImages = mutableListOf<DownloadedImage>() // List to store downloaded images

    // Lazy initialization of the database and DAO
    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private val downloadedImageDao by lazy { db.downloadedImageDao() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.fragment_downloaded_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.downloadedImagesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set up the adapter with a delete function callback
        adapter = DownloadedImagesAdapter(downloadedImages) { downloadedItem ->
            deleteDownloadedImage(downloadedItem)
        }
        recyclerView.adapter = adapter

        // Refresh button: reloads downloaded images
        view.findViewById<Button>(R.id.refreshDownloadedButton)?.setOnClickListener {
            loadDownloadedImages()
        }

        // Back button: navigates back to the previous screen
        view.findViewById<Button>(R.id.backButtonDownloaded)?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Load the downloaded images initially
        loadDownloadedImages()
    }

    /**
     * Loads the list of downloaded images from the database.
     * Runs in the background using coroutines to prevent UI blocking.
     */
    private fun loadDownloadedImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val allDownloaded = downloadedImageDao.getAllDownloadedImages() // Fetch all downloaded images
            downloadedImages.clear()
            downloadedImages.addAll(allDownloaded)
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged() // Notify adapter to refresh the list
            }
        }
    }

    /**
     * Deletes a downloaded image from both the database and local storage.
     * Runs in the background to avoid UI lag.
     *
     * @param item The downloaded image to be deleted.
     */
    private fun deleteDownloadedImage(item: DownloadedImage) {
        CoroutineScope(Dispatchers.IO).launch {
            downloadedImageDao.deleteDownloadedImage(item) // Remove image from database

            // Delete the local image file
            File(item.localPath).delete()

            withContext(Dispatchers.Main) {
                downloadedImages.remove(item)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Downloaded image deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * RecyclerView Adapter for displaying downloaded images.
     *
     * @param items List of downloaded images.
     * @param onDeleteClick Callback function for deleting an image.
     */
    inner class DownloadedImagesAdapter(
        private val items: List<DownloadedImage>,
        private val onDeleteClick: (DownloadedImage) -> Unit
    ) : RecyclerView.Adapter<DownloadedImagesAdapter.ViewHolder>() {

        /**
         * ViewHolder class that holds references to UI elements for each list item.
         */
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView = itemView.findViewById<android.widget.ImageView>(R.id.downloadedImageView) // Image display
            val titleView = itemView.findViewById<android.widget.TextView>(R.id.downloadedImageTitle) // Image title
            val deleteButton = itemView.findViewById<Button>(R.id.deleteDownloadedButton) // Delete button
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_downloaded_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val downloadedItem = items[position]
            holder.titleView.text = downloadedItem.title // Set image title
            val bmp = BitmapFactory.decodeFile(downloadedItem.localPath) // Load image from file path
            holder.imageView.setImageBitmap(bmp) // Set the loaded image in the ImageView
            holder.deleteButton.setOnClickListener { onDeleteClick(downloadedItem) } // Handle delete button click
        }

        override fun getItemCount(): Int = items.size // Returns the number of images in the list
    }
}
