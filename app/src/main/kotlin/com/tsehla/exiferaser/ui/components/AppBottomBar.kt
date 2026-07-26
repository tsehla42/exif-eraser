package com.tsehla.exiferaser.ui.components

import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.tsehla.exiferaser.R

@Composable
fun AppBottomBar(
    onHelp: () -> Unit,
    onGrid: () -> Unit,
    onSettings: () -> Unit,
    isGridSelected: Boolean = false,
    isSettingsSelected: Boolean = false,
    isHelpSelected: Boolean = false,
    containerColor: Color = if (MaterialTheme.colorScheme.background == Color.Black) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    },
) {
    NavigationBar(containerColor = containerColor) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_help_outline_24),
                    contentDescription = stringResource(R.string.help),
                )
            },
            label = { Text(stringResource(R.string.help_short)) },
            selected = isHelpSelected,
            onClick = onHelp,
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_grid_on_24),
                    contentDescription = stringResource(R.string.image_sources),
                )
            },
            label = { Text(stringResource(R.string.grid_short)) },
            selected = isGridSelected,
            onClick = onGrid,
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_settings_24),
                    contentDescription = stringResource(R.string.settings),
                )
            },
            label = { Text(stringResource(R.string.settings_short)) },
            selected = isSettingsSelected,
            onClick = onSettings,
        )
    }
}
