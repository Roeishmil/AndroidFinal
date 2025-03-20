package com.rs.photoshare.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.rs.photoshare.R
import com.rs.photoshare.services.TagSuggestionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagSuggestionFragment : DialogFragment() {
    private lateinit var tagSuggestionService: TagSuggestionService
    private val selectedTags = mutableListOf<String>()

    // Callback interface for returning tags
    interface TagSelectionCallback {
        fun onTagsSelected(tags: List<String>)
    }

    companion object {
        private var tagSelectionCallback: TagSelectionCallback? = null

        fun setTagSelectionCallback(callback: TagSelectionCallback) {
            tagSelectionCallback = callback
        }

        fun clearCallback() {
            tagSelectionCallback = null
        }
    }

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

        // Retrieve existing tags passed via arguments (if any)
        arguments?.getStringArray("selectedTags")?.let { tagsArray ->
            selectedTags.addAll(tagsArray)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_tag_suggestion, null)

        // Initialize UI elements
        postContentEditText = view.findViewById(R.id.editTextPostContent)
        suggestButton = view.findViewById(R.id.buttonGetSuggestions)
        progressBar = view.findViewById(R.id.progressBar)
        chipGroup = view.findViewById(R.id.chipGroupSuggestions)
        errorTextView = view.findViewById(R.id.textError)
        selectedTagsTextView = view.findViewById(R.id.textSelectedTags)
        confirmButton = view.findViewById(R.id.buttonConfirmTags)

        suggestButton.setOnClickListener { getSuggestions() }
        confirmButton.setOnClickListener { confirmTagSelection() }

        // Auto-fill content if provided via arguments and trigger suggestions
        arguments?.getString("inputText")?.let { inputText ->
            postContentEditText.setText(inputText)
            getSuggestions()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun getSuggestions() {
        val userInput = postContentEditText.text.toString().trim()
        if (userInput.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter some content", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch existing tags from local files (or other storage)
        val existingTags = getAllExistingTags()

        progressBar.visibility = android.view.View.VISIBLE
        errorTextView.visibility = android.view.View.GONE
        chipGroup.removeAllViews()

        lifecycleScope.launch {
            val suggestions = tagSuggestionService.suggestTags(userInput, existingTags)
            withContext(Dispatchers.Main) {
                progressBar.visibility = android.view.View.GONE
                if (suggestions.isEmpty()) {
                    errorTextView.text = "No suggestions available. Try a different description or try again later."
                    errorTextView.visibility = android.view.View.VISIBLE
                    return@withContext
                }
                chipGroup.removeAllViews()
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

    // Retrieves existing tags from art metadata files (simplistic implementation)
    private fun getAllExistingTags(): List<String> {
        val tagsSet = mutableSetOf<String>()
        val filesDir = requireContext().filesDir
        filesDir.listFiles { file -> file.name.startsWith("art_") && file.name.endsWith(".json") }
            ?.forEach { file ->
                val jsonContent = file.readText()
                val tagPattern = "\"tags\":\\s*\\[([^\\]]+)\\]".toRegex()
                val match = tagPattern.find(jsonContent)
                match?.groupValues?.getOrNull(1)?.let { tagsJson ->
                    val tagStrings = tagsJson.split(",")
                    tagStrings.forEach { tagString ->
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
        selectedTagsTextView.text = if (selectedTags.isEmpty()) {
            "No tags selected"
        } else {
            "Selected tags: ${selectedTags.joinToString(", ")}"
        }
    }

    private fun confirmTagSelection() {
        try {
            tagSelectionCallback?.onTagsSelected(selectedTags)
            dismiss()
        } catch (e: Exception) {
            Log.e("TagSuggestionFragment", "Error confirming tags: ${e.message}", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
