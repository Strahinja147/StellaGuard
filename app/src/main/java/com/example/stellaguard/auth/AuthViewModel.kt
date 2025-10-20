package com.example.stellaguard.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stellaguard.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }


    fun registerUser(
        email: String,
        pass: String,
        username: String,
        fullName: String,
        phone: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (imageUri == null) {
                _authState.value = AuthState.Error("Molimo izaberite profilnu fotografiju.")
                return@launch
            }

            try {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    _authState.value = AuthState.Error("Kreiranje korisnika nije uspelo.")
                    return@launch
                }

                val imageUrl = uploadProfileImage(imageUri, firebaseUser.uid)
                if (imageUrl == null) {
                    _authState.value = AuthState.Error("Upload fotografije nije uspeo.")
                    firebaseUser.delete().await()
                    return@launch
                }

                // ===== JEDINA IZMENA JE OVDE =====
                val user = User(
                    uid = firebaseUser.uid,
                    username = username.trim(),
                    fullName = fullName.trim(),
                    phoneNumber = phone.trim(),
                    profilePictureUrl = imageUrl,
                    points = 0 // EKSPLICITNO POSTAVLJAMO POENE NA NULU
                )
                // ===================================

                firestore.collection("users").document(firebaseUser.uid).set(user).await()

                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Došlo je do nepoznate greške.")
            }
        }
    }

    private suspend fun uploadProfileImage(uri: Uri, uid: String): String? {
        return try {
            val storageRef = storage.reference.child("profile_pictures/$uid")
            storageRef.putFile(uri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Prijava nije uspela.")
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun saveFcmToken() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w("AuthViewModel", "Korisnik nije ulogovan, prekidam čuvanje tokena.")
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("AuthViewModel", "Preuzimanje FCM tokena nije uspelo", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("AuthViewModel", "Token uspešno preuzet ($token), čuvanje za UID: $uid")

            val userLocationRef = firestore.collection("user_locations").document(uid)
            userLocationRef.set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { Log.d("AuthViewModel", "Token uspešno sačuvan.") }
                .addOnFailureListener { e -> Log.e("AuthViewModel", "Greška pri čuvanju tokena: ", e) }
        }
    }
}