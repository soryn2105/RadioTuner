package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * (RO) Dialog pentru adăugarea manuală a unui post de radio prin URL direct.
 */
@Composable
fun ManualStationDialog(
    initialName: String = "",
    initialUrl: String = "",
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editează post manual" else "Adaugă post manual", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || url.isBlank()) {
                        error = "Te rugăm să completezi ambele câmpuri."
                    } else if (!url.startsWith("http")) {
                        error = "URL-ul trebuie să înceapă cu http:// sau https://"
                    } else {
                        onConfirm(name, url)
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
