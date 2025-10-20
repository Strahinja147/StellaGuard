package com.example.stellaguard.data

import java.util.Date

// Ova data klasa čuva trenutno stanje svih filtera
data class FilterState(
    val byAuthor: String? = null,
    val byTypes: List<LightSourceType> = emptyList(),
    val intensityRange: ClosedFloatingPointRange<Float>? = null,
    val dateRange: Pair<Date, Date>? = null,

    // ===== DODATA POLJA ZA PRETRAGU I RADIJUS =====
    val searchQuery: String? = null,
    val searchRadiusKm: Float? = null // Radijus u kilometrima
)