package com.example.stellaguard.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.stellaguard.R
import com.example.stellaguard.data.Report
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReportListScreen(
    reports: List<Report>,
    onReportClick: (Report) -> Unit
) {
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nema rezultata za izabrane filtere.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reports) { report ->
                ReportListItem(report = report, onClick = { onReportClick(report) })
            }
        }
    }
}

@Composable
fun ReportListItem(
    report: Report,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ===== KOD ZA PRIKAZ SLIKE JE IZMENJEN =====
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(report.imageUrl) // URL slike koju treba učitati
                    .crossfade(true) // Lepa animacija prelaza
                    // Slika koja se prikazuje ako je imageUrl null ili dođe do greške
                    .error(R.drawable.ic_launcher_background) // ZAMENITE SA VAŠOM PODRAZUMEVANOM SLIKOM/IKONOM
                    .build(),
                placeholder = painterResource(R.drawable.ic_launcher_background), // Prikazuje se dok se slika učitava
                contentDescription = "Slika prijave",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            // ===============================================

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Tip: ${report.type}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Autor: ${report.authorUsername}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Jačina: ${report.intensity}", style = MaterialTheme.typography.bodyMedium)

                val formattedDate = report.timestamp?.let {
                    val formatter = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                    formatter.format(it)
                } ?: ""
                Text(text = "Datum: $formattedDate", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}