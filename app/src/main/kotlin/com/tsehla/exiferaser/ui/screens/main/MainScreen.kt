package com.tsehla.exiferaser.ui.screens.main

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import java.io.File
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.core.contract.ActivityResultContractTakePicture
import com.tsehla.exiferaser.core.extension.getDisplayName
import com.tsehla.exiferaser.main.business.MainSideEffect
import com.tsehla.exiferaser.main.business.MainViewModel
import com.tsehla.exiferaser.ui.components.AppBottomBar
import com.tsehla.exiferaser.ui.components.DeleteConfirmationDialog
import com.tsehla.exiferaser.ui.components.ImageSourceCard
import com.tsehla.exiferaser.ui.components.ThemedSnackbarHost
import com.tsehla.exiferaser.ui.components.LoadingState
import com.tsehla.exiferaser.ui.theme.LocalThemeMode

private data class ImageSource(
    val iconResId: Int,
    val labelResId: Int,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToSavePathSelection: () -> Unit,
    onNavigateToProcessing: (String) -> Unit,
    onNavigateToMetadataViewer: (String, String) -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentThemeMode = LocalThemeMode.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedNavItem by remember { mutableIntStateOf(2) }
    var isLauncherActive by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    fun showSnackbar(message: String) {
        snackbarJob?.cancel()
        snackbarJob = coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    val chooseImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { result ->
        isLauncherActive = false
        if (result != null) {
            viewModel.putImageSelection(uri = result)
        }
    }

    val chooseImageLegacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { result ->
        isLauncherActive = false
        if (result != null) {
            viewModel.putImageSelection(uri = result)
        }
    }

    val chooseImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { result ->
        isLauncherActive = false
        if (result.isNotEmpty()) {
            viewModel.putImagesSelection(uris = result)
        }
    }

    val chooseImagesLegacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { result ->
        isLauncherActive = false
        if (result.isNotEmpty()) {
            viewModel.putImagesSelection(uris = result)
        }
    }

    val chooseImageDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { result ->
        isLauncherActive = false
        if (result != null) {
            viewModel.putImageDirectorySelection(uri = result)
        }
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var activeCameraUri by remember { mutableStateOf<Uri?>(null) }

    val launchCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContractTakePicture()
    ) { result ->
        isLauncherActive = false
        if (result != null) {
            viewModel.putImageSelection(uri = result, isFromCamera = true)
        } else {
            activeCameraUri?.let { uri ->
                try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            activeCameraUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLauncherActive = false
        if (isGranted && pendingCameraUri != null) {
            activeCameraUri = pendingCameraUri
            launchCameraLauncher.launch(pendingCameraUri)
        }
        pendingCameraUri = null
    }

    val inspectExifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { result ->
        isLauncherActive = false
        if (result != null) {
            coroutineScope.launch {
                val displayName = context.contentResolver.getDisplayName(result)
                onNavigateToMetadataViewer(result.toString(), displayName)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is MainSideEffect.ImageSources.Image -> {
                    if (!isLauncherActive) {
                        isLauncherActive = true
                        if (sideEffect.isLegacyImageSelectionEnabled) {
                            chooseImageLegacyLauncher.launch(sideEffect.supportedMimeTypes)
                        } else {
                            chooseImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                }

                is MainSideEffect.ImageSources.Images -> {
                    if (!isLauncherActive) {
                        isLauncherActive = true
                        if (sideEffect.isLegacyImageSelectionEnabled) {
                            chooseImagesLegacyLauncher.launch(sideEffect.supportedMimeTypes)
                        } else {
                            chooseImagesLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                }

                is MainSideEffect.ImageSources.ImageDirectory.Success -> {
                    if (!isLauncherActive) {
                        isLauncherActive = true
                        chooseImageDirectoryLauncher.launch(sideEffect.uri)
                    }
                }

                MainSideEffect.ImageSources.ImageDirectory.Failure -> {
                    if (!isLauncherActive) {
                        isLauncherActive = true
                        chooseImageDirectoryLauncher.launch(Uri.EMPTY)
                    }
                }

                is MainSideEffect.ImageSources.Camera.Success -> {
                    if (!isLauncherActive) {
                        isLauncherActive = true
                        pendingCameraUri = sideEffect.uri
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                MainSideEffect.ImageSources.Camera.Failure -> {
                    showSnackbar("Failed to launch camera")
                }

                MainSideEffect.ImageSources.Put.Failure -> {
                    showSnackbar("Failed to save image source configuration")
                }

                MainSideEffect.ImageSources.Put.Success -> {
                    showSnackbar("Successfully saved image source configuration")
                }

                MainSideEffect.Selection.Image.Failure,
                MainSideEffect.Selection.ImageDirectory.Failure,
                MainSideEffect.Selection.Images.Failure -> {
                    showSnackbar("Failed to save image(s)")
                }

                is MainSideEffect.Selection.Image.Success -> {
                    viewModel.chooseSelectionNavigationRoute(isFromCamera = sideEffect.isFromCamera)
                }

                MainSideEffect.Selection.ImageDirectory.Success,
                MainSideEffect.Selection.Images.Success -> {
                    viewModel.chooseSelectionNavigationRoute()
                }

                MainSideEffect.Navigate.ToSettings -> onNavigateToSettings()
                MainSideEffect.Navigate.ToHelp -> onNavigateToHelp()
                MainSideEffect.Navigate.ToSelectionSavePath -> onNavigateToSavePathSelection()
                is MainSideEffect.Navigate.ToSelection -> onNavigateToProcessing(sideEffect.savePath.toString())
                MainSideEffect.Navigate.ToDeleteCameraImages -> {
                    showDeleteConfirmation = true
                }

                MainSideEffect.Images.Delete.Failure -> {
                    showSnackbar("Failed to delete camera images")
                }

                MainSideEffect.Images.Delete.Success -> {
                    showSnackbar("Successfully deleted camera images")
                }

                MainSideEffect.Images.Paste.Failure -> {
                    showSnackbar("Unsupported clipboard content")
                }

                is MainSideEffect.Images.Paste.Success -> {
                    viewModel.handleReceivedImages(sideEffect.uris)
                }

                is MainSideEffect.Images.Received.Single -> {
                    viewModel.putImageSelection(uri = sideEffect.uri)
                }

                is MainSideEffect.Images.Received.Multiple -> {
                    viewModel.putImagesSelection(uris = sideEffect.uris)
                }

                MainSideEffect.Images.Received.None -> {
                    showSnackbar("Unsupported image content")
                }

                is MainSideEffect.ImageSourceReordering -> {
                }

                is MainSideEffect.DefaultNightMode -> {
                }

                MainSideEffect.ImageSources.Initialized -> {
                }

                is MainSideEffect.Shortcut.Handle -> {
                    // TODO: Handle shortcut actions
                }

                is MainSideEffect.Shortcut.ReportUsage -> {
                    // TODO: Report shortcut usage
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.readDefaultValues()
    }

    val imageSources = remember(state.imageSources) {
        listOf(
            ImageSource(
                iconResId = R.drawable.baseline_camera_24,
                labelResId = R.string.camera,
                onClick = { viewModel.launchCamera() },
            ),
            ImageSource(
                iconResId = R.drawable.baseline_photo_album_24,
                labelResId = R.string.image_directory,
                onClick = { viewModel.chooseImageDirectory() },
            ),
            ImageSource(
                iconResId = R.drawable.baseline_search_24,
                labelResId = R.string.inspect_exif,
                onClick = {
                    if (!isLauncherActive) {
                        isLauncherActive = true
                        inspectExifLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
            ),
            ImageSource(
                iconResId = R.drawable.baseline_photo_library_24,
                labelResId = R.string.image_files,
                onClick = { viewModel.chooseImages() },
            ),
        )
    }

    Scaffold(
        snackbarHost = { ThemedSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AppBottomBar(
                onHelp = onNavigateToHelp,
                onGrid = { selectedNavItem = 2 },
                onSettings = onNavigateToSettings,
                isGridSelected = selectedNavItem == 2,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.loadingTasks > 0) {
                LoadingState(modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(0.55f))
                Text(
                    text = stringResource(R.string.choose_your_preferred_image_source_placeholder,
                        stringResource(R.string.choose_your),
                        stringResource(R.string.preferred_image_source)),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )

                OutlinedButton(
                    onClick = { viewModel.handlePasteImages() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_content_paste_24),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.paste_images))
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(
                        items = imageSources,
                        key = { it.labelResId },
                    ) { source ->
                        ImageSourceCard(
                            icon = painterResource(source.iconResId),
                            label = stringResource(source.labelResId),
                            onClick = source.onClick,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteConfirmation = false
                viewModel.deleteCameraImages()
            },
            onDismiss = {
                showDeleteConfirmation = false
            },
        )
    }
}
