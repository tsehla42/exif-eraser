package com.tsehla.exiferaser.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.settings.business.ThemeMode

@Composable
fun ThemeSettings(
    currentThemeMode: ThemeMode,
    useDynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeDisplayOrder = listOf(
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
        ThemeMode.AMOLED to stringResource(R.string.theme_amoled),
    )
    val currentThemeIndex = themeDisplayOrder.indexOfFirst { it.first == currentThemeMode }.coerceAtLeast(0)

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.theme_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(themeDisplayOrder[currentThemeIndex].second)
        Slider(
            value = currentThemeIndex.toFloat(),
            onValueChange = { onThemeModeChange(themeDisplayOrder[it.toInt()].first) },
            valueRange = 0f..(themeDisplayOrder.size - 1).toFloat(),
            steps = themeDisplayOrder.size - 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        GradientArrow(
            startLabel = stringResource(R.string.theme_gradient_light),
            endLabel = stringResource(R.string.theme_gradient_dark),
        )
        Spacer(modifier = Modifier.height(8.dp))

        SwitchPreferenceItem(
            title = stringResource(R.string.dynamic_color),
            subtitle = stringResource(R.string.dynamic_color_description),
            icon = painterResource(R.drawable.baseline_palette_24),
            checked = useDynamicColor,
            onCheckedChange = onDynamicColorChange,
        )
    }
}
