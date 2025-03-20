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

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DownloadedImagesAdapter
    private val downloadedImages = mutableListOf<DownloadedImage>()

    // Obtain the DAO from the database.
    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private val downloadedImageDao by lazy { db.downloadedImageDao() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_downloaded_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.downloadedImagesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DownloadedImagesAdapter(downloadedImages) { downloadedItem ->
            deleteDownloadedImage(downloadedItem)
        }
        recyclerView.adapter = adapter

        // Refresh button: reloads downloaded images.
        view.findViewById<Button>(R.id.refreshDownloadedButton)?.setOnClickListener {
            loadDownloadedImages()
        }

        // Back button: navigates up.
        view.findViewById<Button>(R.id.backButtonDownloaded)?.setOnClickListener {
            findNavController().navigateUp()
        }

        loadDownloadedImages()
    }

    private fun loadDownloadedImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val allDownloaded = downloadedImageDao.getAllDownloadedImages()
            downloadedImages.clear()
            downloadedImages.addAll(allDownloaded)
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun deleteDownloadedImage(item: DownloadedImage) {
        CoroutineScope(Dispatchers.IO).launch {
            downloadedImageDao.deleteDownloadedImage(item)
            // Delete the local image file.
            File(item.localPath).delete()
            withContext(Dispatchers.Main) {
                downloadedImages.remove(item)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Downloaded image deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class DownloadedImagesAdapter(
        private val items: List<DownloadedImage>,
        private val onDeleteClick: (DownloadedImage) -> Unit
    ) : RecyclerView.Adapter<DownloadedImagesAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView = itemView.findViewById<android.widget.ImageView>(R.id.downloadedImageView)
            val titleView = itemView.findViewById<android.widget.TextView>(R.id.downloadedImageTitle)
            val deleteButton = itemView.findViewById<Button>(R.id.deleteDownloadedButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_downloaded_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val downloadedItem = items[position]
            holder.titleView.text = downloadedItem.title
            val bmp = BitmapFactory.decodeFile(downloadedItem.localPath)
            holder.imageView.setImageBitmap(bmp)
            holder.deleteButton.setOnClickListener { onDeleteClick(downloadedItem) }
        }

        override fun getItemCount(): Int = items.size
    }
}
