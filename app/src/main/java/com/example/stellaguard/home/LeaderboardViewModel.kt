package com.example.stellaguard.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stellaguard.data.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // Stanje koje sadrži listu rangiranih korisnika
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    // Stanje za prikazivanje učitavanja
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sastavljamo upit: uzmi sve korisnike, sortiraj ih po polju "points"
                // u opadajućem redosledu (od najvećeg ka najmanjem)
                usersCollection
                    .orderBy("points", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("LeaderboardViewModel", "Greška pri dohvatanju korisnika", error)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            _users.value = snapshot.toObjects(User::class.java)
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("LeaderboardViewModel", "Neuspešno dohvatanje korisnika", e)
                _isLoading.value = false
            }
        }
    }
}