package com.rs.photoshare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rs.photoshare.managers.AuthManager

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        authManager.loginUser(
            email = email,
            password = password,
            onSuccess = { userId ->
                Log.d("LoginActivity", "Login successful for user: $userId")
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                // Navigate to MainActivity or another screen
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            },
            onFailure = { exception ->
                Log.e("LoginActivity", "Login failed: ${exception.message}")
                Toast.makeText(this, "Login failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}
