package com.example.stellaguard.auth

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Helper funkcija za kreiranje URI-ja za sliku sa kamere
fun createImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFile = File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        context.externalCacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    // --- Dozvole ---
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val storagePermissionState = rememberPermissionState(permission = storagePermission)
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    // --- Launcheri ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri = tempCameraUri
            }
        }
    )

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) {
            // =========================================================================
            // !!! NOVA LINIJA DODATA OVDE !!!
            // =========================================================================
            authViewModel.saveFcmToken() // Čuvamo token odmah nakon uspešne registracije

            onRegisterSuccess()
            authViewModel.resetAuthState()
        }
    }

    // --- UI ---
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Kreirajte nalog", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { showDialog = true }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = imageUri ?: "https://via.placeholder.com/150"
                    ),
                    contentDescription = "Profilna slika",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Text("Izaberite ili snimite sliku", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Lozinka") },
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Korisničko ime") })
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Ime i prezime") })
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Broj telefona") })

            Spacer(modifier = Modifier.height(24.dp))

            if (authState is AuthViewModel.AuthState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        authViewModel.registerUser(email, password, username, fullName, phone, imageUri)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registruj se")
                }
            }

            if (authState is AuthViewModel.AuthState.Error) {
                Text(
                    text = (authState as AuthViewModel.AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            TextButton(onClick = onNavigateToLogin) {
                Text("Već imate nalog? Prijavite se")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Izaberite izvor") },
                text = { Text("Odakle želite da dodate sliku?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            if (cameraPermissionState.status.isGranted) {
                                tempCameraUri = createImageUri(context)
                                cameraLauncher.launch(tempCameraUri)
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    ) {
                        Text("Kamera")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            if (storagePermissionState.status.isGranted) {
                                galleryLauncher.launch("image/*")
                            } else {
                                storagePermissionState.launchPermissionRequest()
                            }
                        }
                    ) {
                        Text("Galerija")
                    }
                }
            )
        }
    }
}