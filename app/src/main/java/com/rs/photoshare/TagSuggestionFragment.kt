package com.rs.photoshare.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.rs.photoshare.ImageUploadManager
import com.rs.photoshare.R
import com.rs.photoshare.services.TagSuggestionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TagSuggestionFragment : Fragment() {
    private lateinit var uploadManager: ImageUploadManager
    private lateinit var tagSuggestionService: TagSuggestionService
    private val selectedTags = mutableListOf<String>()
    var callback: TagSelectionCallback? = null

    // UI components
    private lateinit var postContentEditText: EditText
    private lateinit var suggestButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var errorTextView: TextView
    private lateinit var selectedTagsTextView: TextView
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tagSuggestionService = TagSuggestionService(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tag_suggestion, container, false)

        // Initialize UI components
        postContentEditText = view.findViewById(R.id.editTextPostContent)
        suggestButton = view.findViewById(R.id.buttonGetSuggestions)
        progressBar = view.findViewById(R.id.progressBar)
        chipGroup = view.findViewById(R.id.chipGroupSuggestions)
        errorTextView = view.findViewById(R.id.textError)
        selectedTagsTextView = view.findViewById(R.id.textSelectedTags)
        confirmButton = view.findViewById(R.id.buttonConfirmTags)

        // Set up listeners
        suggestButton.setOnClickListener { getSuggestions() }
        confirmButton.setOnClickListener { confirmTagSelection() }

        // Check if we have existing tags to display
        arguments?.getStringArrayList("selectedTags")?.let { tags ->
            selectedTags.addAll(tags)
            updateSelectedTagsView()
        }

        return view
    }

    private fun getSuggestions() {
        val userInput = postContentEditText.text.toString().trim()
        if (userInput.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter some content", Toast.LENGTH_SHORT).show()
            return
        }

        // Get existing tags from the app
        val existingTags = getAllExistingTags()

        // Show loading state
        progressBar.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE
        chipGroup.removeAllViews()

        // Get suggestions
        lifecycleScope.launch {
            val suggestions = tagSuggestionService.suggestTags(userInput, existingTags)

            // Make sure UI updates happen on the main thread
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE

                if (suggestions.isEmpty()) {
                    errorTextView.text =
                        "No suggestions available. Try a different description or try again later."
                    errorTextView.visibility = View.VISIBLE
                    return@withContext
                }

                // Display suggestions as chips
                chipGroup.removeAllViews() // Make sure to clear previous suggestions
                suggestions.forEach { tag ->
                    val chip = Chip(requireContext()).apply {
                        text = tag
                        isCheckable = true
                        isChecked = selectedTags.contains(tag)
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                if (!selectedTags.contains(tag)) {
                                    selectedTags.add(tag)
                                }
                            } else {
                                selectedTags.remove(tag)
                            }
                            updateSelectedTagsView()
                        }
                    }
                    chipGroup.addView(chip)
                }
            }
        }
    }

    private fun getAllExistingTags(): List<String> {
        // This is a simple implementation. In a real app, you'd likely
        // get these from a database or repository.
        val tagsSet = mutableSetOf<String>()
        val filesDir = requireContext().filesDir

        // Look through all art piece metadata files
        filesDir.listFiles { file -> file.name.startsWith("art_") && file.name.endsWith(".json") }
            ?.forEach { file ->
                val jsonContent = file.readText()
                // Simple regex to extract tags from JSON
                val tagPattern = "\"tags\":\\s*\\[([^\\]]+)\\]".toRegex()
                val match = tagPattern.find(jsonContent)
                match?.groupValues?.getOrNull(1)?.let { tagsJson ->
                    // Parse tags from JSON array format
                    val tagStrings = tagsJson.split(",")
                    tagStrings.forEach { tagString ->
                        // Clean up the tag string
                        val cleanTag = tagString.trim().trim('"')
                        if (cleanTag.isNotEmpty()) {
                            tagsSet.add(cleanTag)
                        }
                    }
                }
            }


        return tagsSet.toList()
    }

    private fun updateSelectedTagsView() {
        if (selectedTags.isEmpty()) {
            selectedTagsTextView.text = "No tags selected"
        } else {
            selectedTagsTextView.text = "Selected tags: ${selectedTags.joinToString(", ")}"
        }
    }

    private fun confirmTagSelection() {
        try {
            // Try to use the uploadManager reference
            if (::uploadManager.isInitialized) {
                uploadManager.updateSelectedTags(selectedTags)
                // Go back to previous screen
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "Upload manager not initialized", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("TagSuggestionFragment", "Error confirming tags: ${e.message}", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the input text from arguments if available
        arguments?.getString("inputText")?.let { inputText ->
            postContentEditText.setText(inputText)
            // Automatically trigger suggestions when fragment is created with input text
            getSuggestions()
        }
    }



    interface TagSelectionCallback {
        fun onTagsSelected(tags: List<String>)
    }

    fun setTagSelectionCallback(callback: TagSelectionCallback) {
        this.callback = callback
    }

    companion object {
        fun newInstance(inputText: String): TagSuggestionFragment {
            val fragment = TagSuggestionFragment()
            val args = Bundle()
            args.putString("inputText", inputText)
            fragment.arguments = args
            return fragment
        }
    }
}