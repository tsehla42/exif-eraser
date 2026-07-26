package com.none.tom.exiferaser.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.none.tom.exiferaser.R

data class NightModeOption(
    val value: Int,
    val label: String,
)

@Composable
fun NightModePickerDialog(
    options: List<NightModeOption>,
    selectedValue: Int,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.night_mode))
        },
        text = {
            options.forEachIndexed { index, option ->
                RadioButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(option.value) },
                )
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (index < options.lastIndex) {
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}
