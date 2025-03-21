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
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // UI references
        val nameEditText: EditText = findViewById(R.id.nameEditText)
        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val registerButton: Button = findViewById(R.id.registerButton)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        // Firebase instances
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Handle register button click
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validate input
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show progress bar
            progressBar.visibility = View.VISIBLE

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val user = hashMapOf(
                            "name" to name,
                            "email" to email
                        )

                        // Save user data in Firestore
                        if (userId != null) {
                            firestore.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { exception ->
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this, "Error saving user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Registration failed
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
