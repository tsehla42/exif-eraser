package com.tsehla.exiferaser.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tsehla.exiferaser.settings.business.ThemeMode
import com.tsehla.exiferaser.ui.theme.LocalThemeMode

@Composable
fun ThemedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val currentThemeMode = LocalThemeMode.current
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = when (currentThemeMode) {
                    ThemeMode.AMOLED -> MaterialTheme.colorScheme.surfaceContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = when (currentThemeMode) {
                    ThemeMode.AMOLED -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    )
}
