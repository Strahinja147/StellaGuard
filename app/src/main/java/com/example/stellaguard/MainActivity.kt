package com.example.stellaguard

// Potrebno je dodati ove import naredbe
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.stellaguard.ui.theme.StellaGuardTheme

class MainActivity : ComponentActivity() {

    // 1. Ovaj deo registruje "launcher" koji će obraditi odgovor korisnika na zahtev za dozvolu.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Dozvola je data! Aplikacija sada može da prikazuje notifikacije.
            Log.d("Permissions", "Notification permission granted.")
        } else {
            // Korisnik je odbio dozvolu. Aplikacija neće moći da prikazuje notifikacije.
            // Ovde možete (opciono) prikazati poruku korisniku koja objašnjava zašto je dozvola važna.
            Log.d("Permissions", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Pozivamo našu novu funkciju da proveri i zatraži dozvolu PRE iscrtavanja UI-ja.
        askNotificationPermission()

        setContent {
            StellaGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pozivamo vašu postojeću navigaciju - ovaj deo se ne menja.
                    AppNavigation()
                }
            }
        }
    }

    // 3. Ova funkcija proverava da li je dozvola već data i pokreće zahtev ako nije.
    private fun askNotificationPermission() {
        // Logika se primenjuje samo na Android 13 (API nivo 33, sa kodnim imenom TIRAMISU) i novije.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Proveravamo da li već imamo dozvolu.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Dozvola je već odobrena, ne radimo ništa.
                Log.d("Permissions", "Notification permission already granted.")
            } else {
                // Ako dozvola nije data, pokrećemo sistemski dijalog da je zatražimo.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}