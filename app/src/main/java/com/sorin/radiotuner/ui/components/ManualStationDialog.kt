package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorin.radiotuner.data.model.Station

/**
 * (RO) Dialog pentru adăugarea sau editarea manuală a unui post de radio.
 */
@Composable
fun ManualStationDialog(
    station: Station? = null,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, imageUrl: String, genre: String, country: String, homepage: String) -> Unit
) {
    var name by remember { mutableStateOf(station?.name ?: "") }
    var url by remember { mutableStateOf(station?.streamUrl ?: "") }
    var imageUrl by remember { mutableStateOf(station?.imageUrl ?: "") }
    var genre by remember { mutableStateOf(station?.genre ?: "Manual / Direct") }
    var country by remember { mutableStateOf(station?.country ?: "Local") }
    var homepage by remember { mutableStateOf(station?.homepage ?: "") }
    
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editează post" else "Adaugă post manual", fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Nume post") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("URL Stream") },
                    placeholder = { Text("http://ip:port/stream") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL Logo (Opțional)") },
                    placeholder = { Text("https://exemplu.ro/logo.png") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Gen muzical") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Țară") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = homepage,
                    onValueChange = { homepage = it },
                    label = { Text("Site Web (Opțional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || url.isBlank()) {
                        error = "Te rugăm să completezi Numele și URL-ul."
                    } else if (!url.startsWith("http")) {
                        error = "URL-ul fluxului trebuie să înceapă cu http:// sau https://"
                    } else {
                        onConfirm(name, url, imageUrl, genre, country, homepage)
                    }
                }
            ) {
                Text(if (isEditing) "SALVEAZĂ" else "ADĂUGĂ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANULEAZĂ")
            }
        }
    )
}
