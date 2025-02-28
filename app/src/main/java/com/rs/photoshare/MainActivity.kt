package com.rs.photoshare

import android.os.Bundle
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // Firebase instance
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Find the views
        val userNameText: TextView = findViewById(R.id.userNameText)
        val cameraIcon: ImageView = findViewById(R.id.cameraIcon)
        val logoutButton: Button = findViewById(R.id.logoutButton)

        // Fetch user info from Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userName = document.getString("name") ?: "User"
                        userNameText.text = "Welcome, $userName!"
                    }
                }
        }

        // Set up Logout button
        logoutButton.setOnClickListener {
            auth.signOut()
            // Perform logout logic, e.g., navigate to login screen
        }
    }
}
