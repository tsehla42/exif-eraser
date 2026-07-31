package com.none.tom.exiferaser.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.extension.isNotNullOrEmpty
import com.none.tom.exiferaser.core.image.FileNameMode
import com.none.tom.exiferaser.settings.business.SettingsSideEffect
import com.none.tom.exiferaser.settings.business.SettingsViewModel
import com.none.tom.exiferaser.settings.business.ThemeMode
import com.none.tom.exiferaser.ui.components.DiscreteSliderPreference
import com.none.tom.exiferaser.ui.components.PreferenceItem
import com.none.tom.exiferaser.ui.components.SwitchPreferenceItem
import com.none.tom.exiferaser.ui.components.DisplayNameSuffixDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showDisplayNameSuffix by remember { mutableStateOf(false) }
    var showClearCacheConfirmation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun getCacheSize(): String {
        val cacheDir = context.cacheDir
        val size = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    var cacheSize by remember { mutableStateOf(getCacheSize()) }
    var fileNameExampleRandom by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var fileNameExampleImageId by remember { mutableStateOf((1000..9999).random()) }
    var fileNameExampleTimestamp by remember {
        mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")))
    }

    fun refreshFileNameExamples() {
        fileNameExampleRandom = UUID.randomUUID().toString()
        fileNameExampleImageId = (1000..9999).random()
        fileNameExampleTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
    }

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

    val themeSliderPositions = listOf(
        ThemeMode.LIGHT,
        ThemeMode.SYSTEM,
        ThemeMode.DARK,
        ThemeMode.AMOLED,
    )
    val themePositionLabels = listOf(
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_dark),
        stringResource(R.string.theme_amoled),
    )
    val themeCurrentPosition = themeSliderPositions.indexOf(state.themeMode).coerceAtLeast(0)

    val fileNameSliderLabels = listOf(
        stringResource(R.string.file_name_random),
        stringResource(R.string.file_name_image_id),
        stringResource(R.string.file_name_conversion_date),
        stringResource(R.string.file_name_last_modified),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                PreferenceItem(
                    title = stringResource(R.string.settings_clear_cache),
                    subtitle = cacheSize,
                    icon = Icons.Outlined.DeleteSweep,
                    onClick = { showClearCacheConfirmation = true },
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_file_name),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            item {
                DiscreteSliderPreference(
                    position = state.fileNameMode.ordinal,
                    positionLabels = fileNameSliderLabels,
                    dynamicLabel = fileNameSliderLabels[state.fileNameMode.ordinal],
                    gradientStartLabel = stringResource(R.string.file_name_gradient_private),
                    gradientEndLabel = stringResource(R.string.file_name_gradient_less_private),
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onSurface,
                    ),
                    onPositionChange = { position ->
                        viewModel.storeFileNameMode(FileNameMode.entries[position])
                        refreshFileNameExamples()
                    },
                    descriptionContent = {
                        Column(modifier = Modifier.padding(start = 24.dp, top = 4.dp)) {
                            Text(
                                text = stringResource(
                                    when (state.fileNameMode) {
                                        FileNameMode.RANDOM -> R.string.file_name_slider_description_random
                                        FileNameMode.IMAGE_ID -> R.string.file_name_slider_description_image_id
                                        FileNameMode.CONVERSION_DATE -> R.string.file_name_slider_description_conversion
                                        FileNameMode.LAST_MODIFIED -> R.string.file_name_slider_description_last_modified
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    when (state.fileNameMode) {
                                        FileNameMode.RANDOM -> R.string.file_name_example_random
                                        FileNameMode.IMAGE_ID -> R.string.file_name_example_image_id
                                        FileNameMode.CONVERSION_DATE -> R.string.file_name_example_conversion
                                        FileNameMode.LAST_MODIFIED -> R.string.file_name_example_last_modified
                                    },
                                    when (state.fileNameMode) {
                                        FileNameMode.RANDOM -> fileNameExampleRandom
                                        FileNameMode.IMAGE_ID -> fileNameExampleImageId
                                        FileNameMode.CONVERSION_DATE -> fileNameExampleTimestamp
                                        FileNameMode.LAST_MODIFIED -> fileNameExampleTimestamp
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
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
                    text = stringResource(R.string.settings_section_image),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                    title = stringResource(R.string.settings_auto_delete),
                    icon = Icons.Outlined.AutoDelete,
                    checked = state.isAutoDeleteEnabled,
                    onCheckedChange = { viewModel.storeAutoDelete(it) },
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
                SwitchPreferenceItem(
                    title = stringResource(R.string.settings_share_by_default),
                    icon = Icons.Outlined.Share,
                    checked = state.isShareByDefaultEnabled,
                    onCheckedChange = { viewModel.storeShareByDefault(it) },
                )
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
                DiscreteSliderPreference(
                    position = themeCurrentPosition,
                    positionLabels = themePositionLabels,
                    dynamicLabel = themePositionLabels[themeCurrentPosition],
                    gradientStartLabel = stringResource(R.string.theme_gradient_light),
                    gradientEndLabel = stringResource(R.string.theme_gradient_dark),
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF111111),
                    ),
                    showPositionLabels = false,
                    onPositionChange = { position ->
                        viewModel.storeThemeMode(themeSliderPositions[position])
                    },
                )
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

    if (showClearCacheConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmation = false },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text(stringResource(R.string.clear_cache_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheConfirmation = false
                    context.cacheDir.deleteRecursively()
                    context.cacheDir.mkdirs()
                    cacheSize = getCacheSize()
                    scope.launch { snackbarHostState.showSnackbar("Cache cleared") }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
