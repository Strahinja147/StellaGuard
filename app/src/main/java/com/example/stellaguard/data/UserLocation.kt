package com.example.stellaguard.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserLocation(
    val uid: String = "",
    val location: GeoPoint? = null,
    @ServerTimestamp
    val lastUpdated: Date? = null,
    // Dodajemo i fcmToken, jer ga sada koristimo u bazi
    val fcmToken: String = ""
)