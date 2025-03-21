package com.rs.photoshare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

// HomeFragment is a simple fragment that inflates the home screen layout.
class HomeFragment : Fragment() {

    // Called to inflate the layout for this fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout resource file 'fragment_home.xml'
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
}
