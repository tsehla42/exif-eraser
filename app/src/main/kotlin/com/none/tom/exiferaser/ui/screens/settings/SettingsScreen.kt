package com.none.tom.exiferaser.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.extension.isNotNullOrEmpty
import com.none.tom.exiferaser.settings.business.SettingsSideEffect
import com.none.tom.exiferaser.settings.business.SettingsViewModel
import com.none.tom.exiferaser.settings.business.ThemeMode
import com.none.tom.exiferaser.ui.components.PreferenceItem
import com.none.tom.exiferaser.ui.components.SwitchPreferenceItem
import com.none.tom.exiferaser.ui.components.DisplayNameSuffixDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showDisplayNameSuffix by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val defaultPathOpenLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri.isNotNullOrEmpty()) {
            viewModel.storeDefaultPathOpen(uri)
        }
    }

    val defaultPathSaveLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri.isNotNullOrEmpty()) {
            viewModel.storeDefaultPathSave(uri)
        }
    }

    val themeModeOptions = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
        ThemeMode.AMOLED to stringResource(R.string.theme_amoled),
    )

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is SettingsSideEffect.Navigate.ToDefaultDisplayNameSuffix -> {
                    showDisplayNameSuffix = true
                }

                is SettingsSideEffect.DefaultOpenPath.Select.Success -> {
                    defaultPathOpenLauncher.launch(sideEffect.uri)
                }

                SettingsSideEffect.DefaultOpenPath.Select.Failure -> {
                    defaultPathOpenLauncher.launch(Uri.EMPTY)
                }

                is SettingsSideEffect.DefaultSavePath.Select.Success -> {
                    defaultPathSaveLauncher.launch(sideEffect.uri)
                }

                SettingsSideEffect.DefaultSavePath.Select.Failure -> {
                    defaultPathSaveLauncher.launch(Uri.EMPTY)
                }

                else -> {}
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val isAmoled = MaterialTheme.colorScheme.background == Color.Black
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                ),
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
                    onClick = { viewModel.handleDefaultPathOpen() },
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.settings_default_save_path),
                    subtitle = state.defaultSavePathName.ifBlank { stringResource(R.string.settings_not_set) },
                    icon = Icons.Outlined.Folder,
                    onClick = { viewModel.handleDefaultPathSave() },
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
                SwitchPreferenceItem(
                    title = stringResource(R.string.settings_legacy_image_selection),
                    icon = Icons.Outlined.PhotoLibrary,
                    checked = state.isLegacyImageSelectionEnabled,
                    onCheckedChange = { viewModel.storeLegacyImageSelection(it) },
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
                SwitchPreferenceItem(
                    title = stringResource(R.string.settings_preserve_orientation),
                    icon = Icons.Outlined.Image,
                    checked = state.isPreserveOrientationEnabled,
                    onCheckedChange = { viewModel.storePreserveOrientation(it) },
                )
            }

            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.settings_share_by_default),
                    icon = Icons.Outlined.Share,
                    checked = state.isShareByDefaultEnabled,
                    onCheckedChange = { viewModel.storeShareByDefault(it) },
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
                Text(
                    text = stringResource(R.string.theme_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            item {
                themeModeOptions.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                viewModel.storeThemeMode(mode)
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.themeMode == mode,
                            onClick = {
                                viewModel.storeThemeMode(mode)
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.dynamic_color),
                    subtitle = stringResource(R.string.dynamic_color_description),
                    icon = Icons.Outlined.Palette,
                    checked = state.useDynamicColor,
                    onCheckedChange = {
                        viewModel.storeDynamicColor(it)
                    },
                )
            }
        }
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
