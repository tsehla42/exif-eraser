package com.tsehla.exiferaser.ui.components

import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tsehla.exiferaser.core.extension.copyToClipboard

@Composable
fun CopyToClipboardButton(
    text: String,
    label: String = "Copy",
) {
    val context = LocalContext.current
    Button(
        onClick = {
            context.copyToClipboard("metadata", text)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        },
    ) {
        Text(text = label)
    }
}
