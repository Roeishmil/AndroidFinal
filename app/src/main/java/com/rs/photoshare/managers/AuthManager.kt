package com.rs.photoshare.managers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rs.photoshare.models.User

// AuthManager handles user registration, login, and logout.
class AuthManager {

    // FirebaseAuth instance.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Firestore instance.
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Register a new user and save user data to Firestore.
    fun registerUser(
        email: String,
        password: String,
        name: String,
        profilePictureUrl: String = "",
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val newUser = User(
                        userId = userId,
                        name = name,
                        email = email,
                        profilePictureUrl = profilePictureUrl,
                        uploadedArtPiece = emptyList<String>() // Updated field
                    )
                    firestore.collection("users").document(userId).set(newUser)
                        .addOnSuccessListener { onSuccess(userId) }
                        .addOnFailureListener { exception -> onFailure(exception) }
                } else {
                    task.exception?.let { onFailure(it) }
                }
            }
    }

    // Log in an existing user.
    fun loginUser(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    onSuccess(userId)
                } else {
                    task.exception?.let { onFailure(it) }
                }
            }
    }

    // Get the current user ID (null if not logged in).
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Log out the current user.
    fun logout() {
        auth.signOut()
    }
}
