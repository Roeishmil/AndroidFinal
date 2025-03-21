package com.rs.photoshare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

// LoginActivity handles user authentication using FirebaseAuth.
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Load the login layout

        val emailEditText: EditText = findViewById(R.id.emailEditText) // Input for user email
        val passwordEditText: EditText = findViewById(R.id.passwordEditText) // Input for user password
        val loginButton: Button = findViewById(R.id.loginButton) // Button to perform login
        val registerButton: Button = findViewById(R.id.registerButton) // Button to navigate to registration
        val progressBar: ProgressBar = findViewById(R.id.progressBar) // Shows loading during login

        val auth = FirebaseAuth.getInstance() // Get FirebaseAuth instance

        // Handle login button click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim() // Get email input
            val password = passwordEditText.text.toString().trim() // Get password input

            if (email.isEmpty() || password.isEmpty()) {
                // Show error if any field is empty
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE // Show progress bar while logging in

            // Attempt to sign in with email and password
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE // Hide progress bar after login attempt
                    if (task.isSuccessful) {
                        // Login success, go to main activity
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Login failed, show error
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Navigate to RegisterActivity when register button is clicked
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
