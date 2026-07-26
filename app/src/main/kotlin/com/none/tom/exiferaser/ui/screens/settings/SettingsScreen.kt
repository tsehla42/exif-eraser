package com.none.tom.exiferaser.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.settings.business.SettingsSideEffect
import com.none.tom.exiferaser.settings.business.SettingsViewModel
import com.none.tom.exiferaser.ui.components.PreferenceItem
import com.none.tom.exiferaser.ui.components.SwitchPreferenceItem
import com.none.tom.exiferaser.ui.components.DisplayNameSuffixDialog
import com.none.tom.exiferaser.ui.components.NightModeOption
import com.none.tom.exiferaser.ui.components.NightModePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showNightModePicker by remember { mutableStateOf(false) }
    var showDisplayNameSuffix by remember { mutableStateOf(false) }
    var selectedNightMode by remember { mutableIntStateOf(0) }

    val nightModeOptions = remember {
        listOf(
            NightModeOption(value = 2, label = "System default"),
            NightModeOption(value = 1, label = "Always"),
            NightModeOption(value = 0, label = "Never"),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is SettingsSideEffect.Navigate.ToDefaultDisplayNameSuffix -> {
                    showDisplayNameSuffix = true
                }

                is SettingsSideEffect.Navigate.ToDefaultNightMode -> {
                    selectedNightMode = sideEffect.defaultNightMode
                    showNightModePicker = true
                }

                else -> {}
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_section_filesystem),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_randomize_file_names),
                    icon = Icons.Outlined.AdsClick,
                    onClick = {},
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_default_open_path),
                    subtitle = state.defaultOpenPathName.ifBlank { stringResource(R.string.settings_not_set) },
                    icon = Icons.Outlined.FolderOpen,
                    onClick = {},
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_default_save_path),
                    subtitle = state.defaultSavePathName.ifBlank { stringResource(R.string.settings_not_set) },
                    icon = Icons.Outlined.Folder,
                    onClick = {},
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_image),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_legacy_image_selection),
                    icon = Icons.Outlined.PhotoLibrary,
                    onClick = {},
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_auto_delete),
                    icon = Icons.Outlined.AutoDelete,
                    onClick = {},
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_preserve_orientation),
                    icon = Icons.Outlined.Image,
                    onClick = {},
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_share_by_default),
                    icon = Icons.Outlined.Share,
                    onClick = {},
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_default_display_name_suffix),
                    subtitle = state.defaultDisplayNameSuffix.ifBlank { stringResource(R.string.settings_not_set) },
                    icon = Icons.Outlined.Description,
                    onClick = { viewModel.handleDefaultDisplayNameSuffix() },
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_user_interface),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (state.defaultSavePathName.isNotBlank()) {
                item {
                    SwitchPreferenceItem(
                        title = stringResource(R.string.settings_skip_save_path_selection),
                        icon = Icons.Outlined.Save,
                        checked = state.isSkipSavePathSelectionEnabled,
                        onCheckedChange = { viewModel.storeSavePathSelectionSkip(it) },
                    )
                }
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_default_night_mode),
                    subtitle = state.defaultNightModeName.ifBlank { stringResource(R.string.settings_not_set) },
                    icon = Icons.Outlined.DarkMode,
                    onClick = { viewModel.handleDefaultNightMode() },
                )
            }
        }
    }

    if (showNightModePicker) {
        NightModePickerDialog(
            options = nightModeOptions,
            selectedValue = selectedNightMode,
            onSelect = { selectedNightMode = it },
            onConfirm = {
                showNightModePicker = false
                viewModel.storeDefaultNightMode(selectedNightMode)
            },
            onDismiss = {
                showNightModePicker = false
            },
        )
    }

    if (showDisplayNameSuffix) {
        DisplayNameSuffixDialog(
            currentSuffix = state.defaultDisplayNameSuffix,
            onConfirm = { suffix ->
                showDisplayNameSuffix = false
                viewModel.storeDefaultDisplayNameSuffix(suffix)
            },
            onDismiss = {
                showDisplayNameSuffix = false
            },
        )
    }
}
