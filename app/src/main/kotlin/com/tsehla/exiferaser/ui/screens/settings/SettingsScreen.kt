package com.tsehla.exiferaser.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.core.extension.isNotNullOrEmpty
import com.tsehla.exiferaser.core.image.FileNameMode
import com.tsehla.exiferaser.settings.business.SettingsSideEffect
import com.tsehla.exiferaser.settings.business.SettingsViewModel
import com.tsehla.exiferaser.settings.business.ThemeMode
import com.tsehla.exiferaser.ui.components.AppBottomBar
import com.tsehla.exiferaser.ui.components.ConfirmDialog
import com.tsehla.exiferaser.ui.components.DiscreteSliderPreference
import com.tsehla.exiferaser.ui.components.ThemeSettings
import com.tsehla.exiferaser.ui.components.PreferenceItem
import com.tsehla.exiferaser.ui.components.SwitchPreferenceItem
import com.tsehla.exiferaser.ui.components.ThemedSnackbarHost
import com.tsehla.exiferaser.ui.components.DisplayNameSuffixDialog
import com.tsehla.exiferaser.ui.theme.LocalThemeMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToHelp: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    var showDisplayNameSuffix by remember { mutableStateOf(false) }
    var showClearCacheConfirmation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    fun showSnackbar(message: String) {
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    val currentThemeMode = LocalThemeMode.current

    fun getCacheSize(): String {
        val cacheDir = context.cacheDir
        val size = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    fun isCacheEmpty(): Boolean {
        val cacheDir = context.cacheDir
        return cacheDir.walkTopDown().filter { it.isFile }.none()
    }

    var cacheSize by remember { mutableStateOf(getCacheSize()) }
    var isCacheCurrentlyEmpty by remember { mutableStateOf(isCacheEmpty()) }
    var fileNameExampleRandom by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var fileNameExampleImageId by remember { mutableStateOf((1000..9999).random()) }
    var fileNameExampleConversion by remember {
        mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")))
    }
    var fileNameExampleLastModified by remember {
        mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
    }

    fun refreshFileNameExamples() {
        fileNameExampleRandom = UUID.randomUUID().toString()
        fileNameExampleImageId = (1000..9999).random()
        fileNameExampleConversion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
        fileNameExampleLastModified = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
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
        snackbarHost = { ThemedSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val isAmoled = MaterialTheme.colorScheme.background == Color.Black
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            AppBottomBar(
                onHelp = onNavigateToHelp,
                onGrid = onNavigateToMain,
                onSettings = { },
                isSettingsSelected = true,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceItem(
                title = stringResource(R.string.settings_default_open_path),
                subtitle = state.defaultOpenPathName.ifBlank { stringResource(R.string.settings_not_set) },
                icon = painterResource(R.drawable.baseline_folder_open_24),
                enabled = state.isLegacyImageSelectionEnabled,
                onClick = {
                    if (state.isLegacyImageSelectionEnabled) {
                        viewModel.handleDefaultPathOpen()
                    } else {
                        showSnackbar(context.getString(R.string.settings_default_open_path_requires_legacy))
                    }
                },
            )

            PreferenceItem(
                title = stringResource(R.string.settings_default_save_path),
                subtitle = state.defaultSavePathName.ifBlank { stringResource(R.string.settings_not_set) },
                icon = painterResource(R.drawable.baseline_folder_24),
                onClick = { viewModel.handleDefaultPathSave() },
                trailingIcon = if (state.defaultSavePathName.isNotBlank()) {
                    {
                        IconButton(onClick = { viewModel.clearDefaultPathSave() }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_close_24),
                                contentDescription = stringResource(R.string.clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else null,
            )

            PreferenceItem(
                title = stringResource(R.string.settings_clear_cache),
                subtitle = cacheSize,
                icon = painterResource(R.drawable.baseline_delete_sweep_24),
                enabled = !isCacheCurrentlyEmpty,
                onClick = {
                    if (isCacheCurrentlyEmpty) {
                        showSnackbar(context.getString(R.string.settings_cache_already_empty))
                    } else {
                        showClearCacheConfirmation = true
                    }
                },
            )

            DiscreteSliderPreference(
                position = state.fileNameMode.ordinal,
                positionLabels = fileNameSliderLabels,
                dynamicLabel = fileNameSliderLabels[state.fileNameMode.ordinal],
                gradientStartLabel = stringResource(R.string.file_name_gradient_private),
                gradientEndLabel = stringResource(R.string.file_name_gradient_less_private),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                onPositionChange = { position ->
                    viewModel.storeFileNameMode(FileNameMode.entries[position])
                    refreshFileNameExamples()
                },
                descriptionContent = {
                    Box(modifier = Modifier.height(92.dp)) {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
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
                                        FileNameMode.CONVERSION_DATE -> fileNameExampleConversion
                                        FileNameMode.LAST_MODIFIED -> fileNameExampleLastModified
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )

            PreferenceItem(
                title = stringResource(R.string.settings_default_display_name_suffix),
                subtitle = state.defaultDisplayNameSuffix.ifBlank { stringResource(R.string.settings_not_set) },
                icon = painterResource(R.drawable.baseline_description_24),
                onClick = { viewModel.handleDefaultDisplayNameSuffix() },
            )

            SwitchPreferenceItem(
                title = stringResource(R.string.settings_preserve_orientation),
                icon = painterResource(R.drawable.baseline_photo_library_24),
                checked = state.isPreserveOrientationEnabled,
                onCheckedChange = { viewModel.storePreserveOrientation(it) },
            )

            SwitchPreferenceItem(
                title = stringResource(R.string.settings_auto_delete),
                icon = painterResource(R.drawable.baseline_auto_delete_24),
                checked = state.isAutoDeleteEnabled,
                onCheckedChange = { viewModel.storeAutoDelete(it) },
            )

            SwitchPreferenceItem(
                title = stringResource(R.string.settings_delete_converted),
                icon = painterResource(R.drawable.baseline_delete_24),
                checked = state.isDeleteConvertedEnabled,
                onCheckedChange = { viewModel.storeDeleteConverted(it) },
            )

            SwitchPreferenceItem(
                title = stringResource(R.string.settings_legacy_image_selection),
                icon = painterResource(R.drawable.baseline_photo_library_24),
                checked = state.isLegacyImageSelectionEnabled,
                onCheckedChange = { viewModel.storeLegacyImageSelection(it) },
            )

            SwitchPreferenceItem(
                title = stringResource(R.string.settings_share_by_default),
                icon = painterResource(R.drawable.baseline_share_24),
                checked = state.isShareByDefaultEnabled,
                onCheckedChange = { viewModel.storeShareByDefault(it) },
            )

            ThemeSettings(
                currentThemeMode = state.themeMode,
                useDynamicColor = state.useDynamicColor,
                onThemeModeChange = { viewModel.storeThemeMode(it) },
                onDynamicColorChange = { viewModel.storeDynamicColor(it) },
            )
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
        ConfirmDialog(
            title = stringResource(R.string.clear_cache),
            message = stringResource(R.string.clear_cache_message),
            confirmButtonText = stringResource(android.R.string.ok),
            dismissButtonText = stringResource(android.R.string.cancel),
            onConfirm = {
                showClearCacheConfirmation = false
                context.cacheDir.deleteRecursively()
                context.cacheDir.mkdirs()
                cacheSize = getCacheSize()
                isCacheCurrentlyEmpty = isCacheEmpty()
                showSnackbar("Cache cleared")
            },
            onDismiss = { showClearCacheConfirmation = false },
        )
    }
}
