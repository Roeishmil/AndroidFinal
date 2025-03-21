package com.rs.photoshare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    // Firebase authentication instance
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Set the splash screen layout

        // Delay execution for 1 second before checking auth state
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, 1000)
    }

    private fun checkAuthState() {
        // Check if the user is already authenticated
        if (auth.currentUser != null) {
            // User is logged in, navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User not logged in, navigate to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        // Finish the splash activity so it's not in the back stack
        finish()
    }
}
