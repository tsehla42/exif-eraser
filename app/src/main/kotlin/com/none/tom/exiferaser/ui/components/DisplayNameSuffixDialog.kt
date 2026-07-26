package com.none.tom.exiferaser.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.util.FileUtilsKt

@Composable
fun DisplayNameSuffixDialog(
    currentSuffix: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var suffix by remember { mutableStateOf(currentSuffix) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.default_display_name_suffix))
        },
        text = {
            OutlinedTextField(
                value = suffix,
                onValueChange = { newValue ->
                    suffix = newValue
                    error = if (newValue.isNotEmpty() && !FileUtilsKt.isValidExtFilename(newValue)) {
                        stringResource(R.string.suffix_invalid)
                    } else {
                        null
                    }
                },
                label = { Text(text = stringResource(R.string.suffix)) },
                isError = error != null,
                supportingText = error?.let { msg ->
                    { Text(text = msg) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(suffix.trim()) },
                enabled = error == null,
            ) {
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
