package com.tsehla.exiferaser.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ShareSelectionDialog(
    title: String,
    message: String,
    shareAllText: String,
    shareConvertedText: String,
    onShareAll: () -> Unit,
    onShareConverted: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = onShareAll) {
                Text(text = shareAllText)
            }
        },
        dismissButton = {
            TextButton(onClick = onShareConverted) {
                Text(text = shareConvertedText)
            }
        },
    )
}
