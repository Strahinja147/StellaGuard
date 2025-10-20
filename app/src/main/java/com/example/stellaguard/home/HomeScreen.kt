package com.example.stellaguard.home

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// NOVI ISPRAVLJENI IMPORT
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stellaguard.BuildConfig
import com.example.stellaguard.auth.AuthViewModel
import com.example.stellaguard.data.Report
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.io.File
import java.util.Objects

private enum class ViewMode { MAP, LIST }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current

    val userLocation by mapViewModel.userLocation.collectAsState()
    val reports by mapViewModel.reports.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(44.7866, 20.4489), 12f)
    }

    var showAddReportDialog by remember { mutableStateOf(false) }
    var reportLocation by remember { mutableStateOf<LatLng?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoChoiceDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    var currentViewMode by remember { mutableStateOf(ViewMode.MAP) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterState by mapViewModel.filterState.collectAsState()

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    val tempUri = remember {
        val picturesDir = File(context.filesDir, "pictures")
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }
        val imageFile = File(picturesDir, "temp_image_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            Objects.requireNonNull(context),
            BuildConfig.APPLICATION_ID + ".provider",
            imageFile
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempUri
        }
    }

    fun resetDialogState() {
        showAddReportDialog = false
        imageUri = null
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(it, 15f),
                durationMs = 1500
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, locationPermissionsState.allPermissionsGranted) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && locationPermissionsState.allPermissionsGranted) {
                mapViewModel.startLocationUpdates(context)
            }
            if (event == Lifecycle.Event.ON_STOP) {
                mapViewModel.stopLocationUpdates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {

        Crossfade(targetState = currentViewMode, label = "ViewModeAnimation") { viewMode ->
            when (viewMode) {
                ViewMode.MAP -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = locationPermissionsState.allPermissionsGranted),
                        onMapLongClick = { latLng ->
                            reportLocation = latLng
                            showAddReportDialog = true
                        }
                    ) {
                        reports.forEach { report ->
                            report.location?.let { geoPoint ->
                                Marker(
                                    state = MarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                                    title = "Tip: ${report.type}",
                                    snippet = "Autor: ${report.authorUsername}",
                                    onClick = {
                                        selectedReport = report
                                        false
                                    }
                                )
                            }
                        }
                    }
                }
                ViewMode.LIST -> {
                    ReportListScreen(
                        reports = reports,
                        onReportClick = { selectedReport = it }
                    )
                }
            }
        }

        if (selectedReport != null) {
            val distance = if (userLocation != null && selectedReport!!.location != null) {
                mapViewModel.calculateDistance(userLocation!!.latitude, userLocation!!.longitude, selectedReport!!.location!!.latitude, selectedReport!!.location!!.longitude)
            } else { Float.MAX_VALUE }

            ReportDetailSheet(
                report = selectedReport!!,
                userLocation = userLocation,
                distance = distance,
                onDismiss = { selectedReport = null },
                onConfirm = { reportToConfirm -> mapViewModel.confirmReport(reportToConfirm) }
            )
        }

        if (showAddReportDialog && reportLocation != null) {
            AddReportDialog(
                imageUri = imageUri,
                onDismissRequest = { resetDialogState() },
                onAddPhotoClick = { showPhotoChoiceDialog = true },
                onConfirm = { type, intensity, suggestion ->
                    mapViewModel.addReport(reportLocation!!, type, intensity, suggestion, imageUri)
                    resetDialogState()
                }
            )
        }

        if (showPhotoChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoChoiceDialog = false },
                title = { Text("Izaberi Izvor") },
                text = { Text("Odakle želite da dodate fotografiju?") },
                confirmButton = { Button(onClick = { if (cameraPermissionState.status.isGranted) cameraLauncher.launch(tempUri) else cameraPermissionState.launchPermissionRequest(); showPhotoChoiceDialog = false }) { Text("Kamera") } },
                dismissButton = { Button(onClick = { galleryLauncher.launch("image/*"); showPhotoChoiceDialog = false }) { Text("Galerija") } }
            )
        }

        if (showFilterSheet) {
            FilterSheet(
                initialState = filterState,
                onDismiss = { showFilterSheet = false },
                onApplyFilters = { newFilters -> mapViewModel.applyFilters(newFilters) },
                onResetFilters = { mapViewModel.resetFilters() }
            )
        }

        if (!locationPermissionsState.allPermissionsGranted) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                    Text("Potrebna je dozvola za lokaciju")
                }
            }
        } else {
            FloatingActionButton(
                onClick = {
                    userLocation?.let {
                        reportLocation = it
                        showAddReportDialog = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj prijavu")
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { mapViewModel.stopLocationUpdates(); authViewModel.logout(); onLogout() }) {
                Text("Odjavi se")
            }

            FloatingActionButton(onClick = onNavigateToLeaderboard) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = "Rang Lista")
            }

            FloatingActionButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Default.FilterAlt, contentDescription = "Filteri")
            }

            FloatingActionButton(onClick = {
                currentViewMode = if (currentViewMode == ViewMode.MAP) ViewMode.LIST else ViewMode.MAP
            }) {
                // ISPRAVLJENA LINIJA
                val icon = if (currentViewMode == ViewMode.MAP) Icons.AutoMirrored.Filled.ViewList else Icons.Default.Map
                Icon(icon, contentDescription = "Promeni prikaz")
            }
        }
    }
}