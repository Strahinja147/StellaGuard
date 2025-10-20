package com.example.stellaguard.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stellaguard.data.FilterState
import com.example.stellaguard.data.LightSourceType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    initialState: FilterState,
    onDismiss: () -> Unit,
    onApplyFilters: (FilterState) -> Unit,
    onResetFilters: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Lokalna stanja unutar panela, inicijalizovana sa početnim stanjem
    var author by remember { mutableStateOf(initialState.byAuthor ?: "") }
    var selectedTypes by remember { mutableStateOf(initialState.byTypes.toSet()) }
    var intensityRange by remember { mutableStateOf(initialState.intensityRange ?: 1f..10f) }
    var searchQuery by remember { mutableStateOf(initialState.searchQuery ?: "") }

    // ===== ISPRAVLJENA LINIJA KODA =====
    // Koristimo 0f da označimo da je filter isključen
    var searchRadiusKm by remember { mutableFloatStateOf(initialState.searchRadiusKm ?: 0f) }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filteri i Pretraga", style = MaterialTheme.typography.headlineSmall)
            Divider()

            // Polje za tekstualnu pretragu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Pretraži po opisu, tipu...") },
                modifier = Modifier.fillMaxWidth()
            )

            // Slajder za pretragu u radijusu
            val radiusLabel = if (searchRadiusKm > 0) {
                "Pretraga u radijusu: ${searchRadiusKm.roundToInt()} km"
            } else {
                "Pretraga u radijusu: Isključeno"
            }
            Text(radiusLabel, style = MaterialTheme.typography.titleMedium)
            Slider(
                value = searchRadiusKm,
                onValueChange = { searchRadiusKm = it },
                valueRange = 0f..50f, // 0 do 50 km
                steps = 49 // Omogućava biranje celih brojeva
            )

            // Filter po autoru
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Ime autora") },
                modifier = Modifier.fillMaxWidth()
            )

            // Filter po tipu
            Text("Tip izvora:", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LightSourceType.entries) { type ->
                    FilterChip(
                        selected = selectedTypes.contains(type),
                        onClick = {
                            selectedTypes = if (selectedTypes.contains(type)) {
                                selectedTypes - type
                            } else {
                                selectedTypes + type
                            }
                        },
                        label = { Text(type.name) }
                    )
                }
            }

            // Filter po jačini
            Text("Opseg jačine: ${intensityRange.start.toInt()} - ${intensityRange.endInclusive.toInt()}",
                style = MaterialTheme.typography.titleMedium)
            RangeSlider(
                value = intensityRange,
                onValueChange = { intensityRange = it },
                valueRange = 1f..10f,
                steps = 8
            )

            // Dugmad za akciju
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    onResetFilters()
                    onDismiss()
                }) {
                    Text("Poništi sve")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val newFilterState = FilterState(
                        byAuthor = author.takeIf { it.isNotBlank() },
                        byTypes = selectedTypes.toList(),
                        intensityRange = intensityRange,
                        dateRange = initialState.dateRange, // Zadržavamo postojeći dateRange
                        searchQuery = searchQuery.takeIf { it.isNotBlank() },
                        searchRadiusKm = searchRadiusKm.takeIf { it > 0f }
                    )
                    onApplyFilters(newFilterState)
                    onDismiss()
                }) {
                    Text("Primeni")
                }
            }
        }
    }
}