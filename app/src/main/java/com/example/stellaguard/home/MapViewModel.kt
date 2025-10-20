package com.example.stellaguard.home

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stellaguard.data.FilterState
import com.example.stellaguard.data.LightSourceType
import com.example.stellaguard.data.Report
import com.example.stellaguard.data.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class MapViewModel : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val reportsCollection = firestore.collection("reports")
    private val usersCollection = firestore.collection("users")
    private val locationsCollection = firestore.collection("user_locations")

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports = _reports.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.asStateFlow()

    private var reportsListener: ListenerRegistration? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationCallback: LocationCallback

    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let { location ->
                    _userLocation.value = LatLng(location.latitude, location.longitude)
                    updateUserLocationInFirestore(LatLng(location.latitude, location.longitude))
                }
            }
        }
        fetchReports()
    }

    private fun fetchReports() {
        reportsListener?.remove()

        var query: Query = reportsCollection

        val filters = _filterState.value

        // PRIMENJUJEMO SERVER-SIDE FILTERE (one koje Firestore podržava efikasno)
        if (!filters.byAuthor.isNullOrBlank()) {
            query = query.whereEqualTo("authorUsername", filters.byAuthor)
        }
        filters.intensityRange?.let {
            query = query.whereGreaterThanOrEqualTo("intensity", it.start)
            query = query.whereLessThanOrEqualTo("intensity", it.endInclusive)
        }
        // Filter po datumu bi išao ovde ako ga implementiraš

        reportsListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("MapViewModel", "Greška pri dohvatanju izveštaja", error)
                return@addSnapshotListener
            }

            val serverFilteredReports = snapshot?.toObjects(Report::class.java) ?: emptyList()

            // ===== START: NOVA LOGIKA ZA CLIENT-SIDE FILTRIRANJE =====

            // 1. Filtriranje po tipu (kao i pre)
            val reportsByType = if (filters.byTypes.isNotEmpty()) {
                val selectedTypeNames = filters.byTypes.map { it.name }
                serverFilteredReports.filter { report -> report.type in selectedTypeNames }
            } else {
                serverFilteredReports
            }

            // 2. Filtriranje po radijusu
            val currentUserLocation = _userLocation.value
            val radiusInMeters = filters.searchRadiusKm?.let { it * 1000 }

            val reportsByRadius = if (radiusInMeters != null && currentUserLocation != null) {
                reportsByType.filter { report ->
                    report.location?.let { geoPoint ->
                        calculateDistance(
                            currentUserLocation.latitude,
                            currentUserLocation.longitude,
                            geoPoint.latitude,
                            geoPoint.longitude
                        ) <= radiusInMeters
                    } ?: false
                }
            } else {
                reportsByType
            }

            // 3. Filtriranje po unetom tekstu za pretragu
            val finalReports = if (!filters.searchQuery.isNullOrBlank()) {
                val searchText = filters.searchQuery.lowercase(Locale.getDefault())
                reportsByRadius.filter { report ->
                    // Pretražujemo u bitnim poljima, ignorišemo velika/mala slova
                    report.authorUsername.lowercase(Locale.getDefault()).contains(searchText) ||
                            report.type.lowercase(Locale.getDefault()).contains(searchText) ||
                            report.suggestion.lowercase(Locale.getDefault()).contains(searchText)
                }
            } else {
                reportsByRadius
            }

            // ===== END: NOVA LOGIKA ZA CLIENT-SIDE FILTRIRANJE =====

            _reports.value = finalReports
            Log.d("MapViewModel", "Dohvaćeno ${finalReports.size} izveštaja nakon svih filtera.")
        }
    }


    fun applyFilters(newFilterState: FilterState) {
        _filterState.value = newFilterState
        fetchReports()
    }

    fun resetFilters() {
        _filterState.value = FilterState()
        fetchReports()
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun updateUserLocationInFirestore(latLng: LatLng) {
        val uid = auth.currentUser?.uid ?: return
        val locationMap = mapOf("latitude" to latLng.latitude, "longitude" to latLng.longitude)
        val userLocationData = mapOf(
            "uid" to uid,
            "location" to locationMap,
            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        locationsCollection.document(uid).set(userLocationData, com.google.firebase.firestore.SetOptions.merge())
    }

    private suspend fun uploadReportImage(imageUri: Uri): String? {
        return try {
            val fileName = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("report_images/$fileName.jpg")
            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("MapViewModel", "Greška pri uploadu slike", e)
            null
        }
    }

    fun addReport(location: LatLng, type: LightSourceType, intensity: Int, suggestion: String, imageUri: Uri?) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val reportId = reportsCollection.document().id
            var imageUrl: String? = null

            if (imageUri != null) {
                imageUrl = uploadReportImage(imageUri)
            }

            try {
                val userDoc = usersCollection.document(userId).get().await()
                val user = userDoc.toObject(User::class.java)
                val authorUsername = user?.username ?: "Nepoznat autor"

                val newReport = Report(
                    id = reportId,
                    userId = userId,
                    authorUsername = authorUsername,
                    location = GeoPoint(location.latitude, location.longitude),
                    type = type.name,
                    intensity = intensity,
                    suggestion = suggestion,
                    imageUrl = imageUrl,
                    confirmations = emptyList()
                )

                reportsCollection.document(reportId).set(newReport).await()
                Log.d("MapViewModel", "Uspešno dodat novi izveštaj!")
                updateUserPoints(userId, 10)

                resetFilters()

            } catch (e: Exception) {
                Log.e("MapViewModel", "Greška pri dodavanju izveštaja", e)
            }
        }
    }

    private fun updateUserPoints(userId: String, pointsToAdd: Long) {
        val userRef = usersCollection.document(userId)
        userRef.update("points", FieldValue.increment(pointsToAdd))
            .addOnSuccessListener { Log.d("MapViewModel", "Uspešno dodato $pointsToAdd poena korisniku $userId") }
            .addOnFailureListener { e -> Log.e("MapViewModel", "Greška pri dodavanju poena", e) }
    }

    fun confirmReport(report: Report) {
        val userId = auth.currentUser?.uid ?: return
        if (report.confirmations.contains(userId) || report.userId == userId) {
            Log.d("MapViewModel", "Korisnik je autor ili je već potvrdio ovaj izveštaj.")
            return
        }

        viewModelScope.launch {
            try {
                reportsCollection.document(report.id)
                    .update("confirmations", FieldValue.arrayUnion(userId))
                    .await()
                updateUserPoints(userId, 2)
                Log.d("MapViewModel", "Izveštaj ${report.id} uspešno potvrđen od strane $userId")
            } catch (e: Exception) {
                Log.e("MapViewModel", "Greška pri potvrdi izveštaja", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        reportsListener?.remove()
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}