package com.example.stellaguard.home

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.stellaguard.data.LightSourceType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportDialog(
    imageUri: Uri?,
    onDismissRequest: () -> Unit,
    onConfirm: (type: LightSourceType, intensity: Int, suggestion: String) -> Unit,
    onAddPhotoClick: () -> Unit
) {
    var suggestion by remember { mutableStateOf("") }
    var sliderPosition by remember { mutableFloatStateOf(5f) }
    var selectedType by remember { mutableStateOf(LightSourceType.STREET_LIGHT) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Dodaj Izvor Zagađenja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Odabrana slika",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Button(onClick = onAddPhotoClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Dodaj Fotografiju")
                    }
                }

                OutlinedTextField(
                    value = suggestion,
                    onValueChange = { suggestion = it },
                    label = { Text("Predlog za poboljšanje") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedType.name,
                        onValueChange = {},
                        label = { Text("Tip Izvora") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        LightSourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(text = "Jačina: ${sliderPosition.roundToInt()}", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedType, sliderPosition.roundToInt(), suggestion)
                }
            ) {
                Text("Potvrdi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Odustani")
            }
        }
    )
}