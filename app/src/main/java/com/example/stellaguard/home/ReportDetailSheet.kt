package com.example.stellaguard.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.stellaguard.data.Report
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

const val CHECK_IN_RADIUS_METERS = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailSheet(
    report: Report,
    userLocation: LatLng?,
    distance: Float,
    onDismiss: () -> Unit,
    onConfirm: (Report) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val currentUser = Firebase.auth.currentUser

    val isUserNearby = userLocation != null && distance <= CHECK_IN_RADIUS_METERS
    val canConfirm = currentUser?.uid != report.userId && !report.confirmations.contains(currentUser?.uid)


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (report.imageUrl != null) {
                AsyncImage(
                    model = report.imageUrl,
                    contentDescription = "Fotografija izvora zagađenja",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally)
                ) {
                    Text("Nema dostupne fotografije", modifier = Modifier.align(Alignment.Center))
                }
            }

            Text("Detalji Prijave", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Divider()
            DetailRow(label = "Autor:", value = report.authorUsername)
            DetailRow(label = "Tip izvora:", value = report.type)
            DetailRow(label = "Prijavljena jačina:", value = report.intensity.toString())
            DetailRow(label = "Predlog:", value = report.suggestion.ifEmpty { "Nije unet." })
            DetailRow(label = "Broj potvrda:", value = report.confirmations.size.toString())

            val formattedDate = report.timestamp?.let {
                val formatter = SimpleDateFormat("dd.MM.yyyy. 'u' HH:mm", Locale.getDefault())
                formatter.format(it)
            } ?: "Nepoznat datum"
            DetailRow(label = "Prijavljeno:", value = formattedDate)

            // ===== REVIDIRANA LOGIKA ZA PRIKAZ TEKSTA =====
            // Prikazujemo informaciju o udaljenosti samo ako korisnik može da potvrdi
            // i ako je zaista blizu lokacije.
            if (canConfirm && isUserNearby) {
                Text(
                    text = "Nalazite se u zoni za potvrdu (%.0f metara).".format(distance),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            // ===============================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canConfirm) {
                    Button(
                        onClick = {
                            onConfirm(report)
                            onDismiss()
                        },
                        enabled = isUserNearby
                    ) {
                        Text("Potvrdi Izvor")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(onClick = onDismiss) {
                    Text("Zatvori")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(text = value, modifier = Modifier.weight(2f))
    }
}