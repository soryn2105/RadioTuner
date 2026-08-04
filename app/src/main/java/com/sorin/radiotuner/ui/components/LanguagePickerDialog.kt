package com.sorin.radiotuner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sorin.radiotuner.R
import com.sorin.radiotuner.util.AppLanguage

/**
 * (RO)
 * Dialog declanșat de butonul de limbă din header. Alegerea NU
 * afectează doar interfața momentan afișată — apelează
 * LanguageManager.setLanguage(...), care recreează activitatea cu noua
 * limbă (comportament standard Android la schimbare de configurare).
 * (EN)
 * Dialog triggered by the language button in the header. The choice does NOT
 * only affect the currently displayed interface — it calls
 * LanguageManager.setLanguage(...), which recreates the activity with the new
 * language (standard Android behavior on configuration change).
 */

@Composable
fun LanguagePickerDialog(
    currentTag: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_picker_title)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    val label = when (language) {
                        AppLanguage.ROMANIAN -> stringResource(R.string.language_romanian)
                        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                        AppLanguage.FRENCH -> stringResource(R.string.language_french)
                        AppLanguage.GERMAN -> stringResource(R.string.language_german)
                        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentTag == language.tag,
                                onClick = { onLanguageSelected(language) }
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTag == language.tag,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}