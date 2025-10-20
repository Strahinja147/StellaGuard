package com.example.stellaguard.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// VAŽNO: Enum se deklariše OVDE, izvan data klase, ali u istom fajlu.
// Ovo ga čini vidljivim za sve ostale klase u projektu.
enum class LightSourceType {
    STREET_LIGHT, // Ulično svetlo
    BILLBOARD,    // Reklama
    REFLECTOR,    // Reflektor
    OTHER         // Ostalo
}

data class Report(
    // Polja koja si već imao:
    val id: String = "",
    val userId: String = "", // ID korisnika koji je kreirao
    val location: GeoPoint? = null,
    @ServerTimestamp
    val timestamp: Date? = null,

    // SVA NOVA POLJA prema temi aplikacije:
    val authorUsername: String = "", // Ime korisnika koji je kreirao (radi lakšeg prikaza)
    val type: String = LightSourceType.OTHER.name, // Tip izvora, čuvamo kao String
    val intensity: Int = 1, // Jačina svetla (1-10)
    val suggestion: String = "", // Predlog za poboljšanje (preimenovano iz description)
    val imageUrl: String? = null, // URL do fotografije u Firebase Storage-u
    val confirmations: List<String> = emptyList() // Lista userId-jeva koji su potvrdili
)