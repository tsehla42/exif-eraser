package com.none.tom.exiferaser.ui.screens.main

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.contract.ActivityResultContractTakePicture
import com.none.tom.exiferaser.main.business.MainSideEffect
import com.none.tom.exiferaser.main.business.MainViewModel
import com.none.tom.exiferaser.ui.components.DeleteConfirmationDialog
import com.none.tom.exiferaser.ui.components.ImageSourceCard
import com.none.tom.exiferaser.ui.components.LoadingState

private data class ImageSource(
    val icon: ImageVector,
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedNavItem by remember { mutableIntStateOf(2) }

    val chooseImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { result ->
        viewModel.putImageSelection(uri = result)
    }

    val chooseImageLegacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { result ->
        viewModel.putImageSelection(uri = result)
    }

    val chooseImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { result ->
        viewModel.putImagesSelection(uris = result)
    }

    val chooseImagesLegacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { result ->
        viewModel.putImagesSelection(uris = result)
    }

    val chooseImageDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { result ->
        viewModel.putImageDirectorySelection(uri = result)
    }

    val launchCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContractTakePicture()
    ) { result ->
        viewModel.putImageSelection(uri = result, isFromCamera = true)
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingCameraUri != null) {
            launchCameraLauncher.launch(pendingCameraUri)
        }
        pendingCameraUri = null
    }

    val inspectExifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { result ->
        if (result != null) {
            onNavigateToMetadataViewer(result.toString(), result.lastPathSegment ?: "")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is MainSideEffect.ImageSources.Image -> {
                    if (sideEffect.isLegacyImageSelectionEnabled) {
                        chooseImageLegacyLauncher.launch(sideEffect.supportedMimeTypes)
                    } else {
                        chooseImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }

                is MainSideEffect.ImageSources.Images -> {
                    if (sideEffect.isLegacyImageSelectionEnabled) {
                        chooseImagesLegacyLauncher.launch(sideEffect.supportedMimeTypes)
                    } else {
                        chooseImagesLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }

                is MainSideEffect.ImageSources.ImageDirectory.Success -> {
                    chooseImageDirectoryLauncher.launch(sideEffect.uri)
                }

                MainSideEffect.ImageSources.ImageDirectory.Failure -> {
                    chooseImageDirectoryLauncher.launch(Uri.EMPTY)
                }

                is MainSideEffect.ImageSources.Camera.Success -> {
                    pendingCameraUri = sideEffect.uri
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }

                MainSideEffect.ImageSources.Camera.Failure -> {
                    snackbarHostState.showSnackbar(
                        message = "Failed to launch camera"
                    )
                }

                MainSideEffect.ImageSources.Put.Failure -> {
                    snackbarHostState.showSnackbar(
                        message = "Failed to save image source configuration"
                    )
                }

                MainSideEffect.ImageSources.Put.Success -> {
                    snackbarHostState.showSnackbar(
                        message = "Successfully saved image source configuration"
                    )
                }

                MainSideEffect.Selection.Image.Failure,
                MainSideEffect.Selection.ImageDirectory.Failure,
                MainSideEffect.Selection.Images.Failure -> {
                    snackbarHostState.showSnackbar(
                        message = "Failed to save image(s)"
                    )
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
                    snackbarHostState.showSnackbar(
                        message = "Failed to delete camera images"
                    )
                }

                MainSideEffect.Images.Delete.Success -> {
                    snackbarHostState.showSnackbar(
                        message = "Successfully deleted camera images"
                    )
                }

                MainSideEffect.Images.Paste.Failure -> {
                    snackbarHostState.showSnackbar(
                        message = "Unsupported clipboard content"
                    )
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
                    snackbarHostState.showSnackbar(
                        message = "Unsupported clipboard content"
                    )
                }

                is MainSideEffect.ImageSourceReordering -> {
                    // Reorder handled by ViewModel internally
                }

                is MainSideEffect.DefaultNightMode -> {
                    // Night mode handled via system settings in Compose
                }

                MainSideEffect.ImageSources.Initialized -> {
                    // Consumed
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
                icon = Icons.Outlined.CameraAlt,
                labelResId = R.string.camera,
                onClick = { viewModel.launchCamera() },
            ),
            ImageSource(
                icon = Icons.Outlined.PhotoAlbum,
                labelResId = R.string.image_directory,
                onClick = { viewModel.chooseImageDirectory() },
            ),
            ImageSource(
                icon = Icons.Outlined.Search,
                labelResId = R.string.inspect_exif,
                onClick = { inspectExifLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                ) },
            ),
            ImageSource(
                icon = Icons.Outlined.PhotoLibrary,
                labelResId = R.string.image_files,
                onClick = { viewModel.chooseImages() },
            ),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = if (MaterialTheme.colorScheme.background == Color.Black) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.help),
                        )
                    },
                    label = { Text(stringResource(R.string.help_short)) },
                    selected = false,
                    onClick = onNavigateToHelp,
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = stringResource(R.string.paste_images),
                        )
                    },
                    label = { Text(stringResource(R.string.paste_short)) },
                    selected = false,
                    onClick = { viewModel.handlePasteImages() },
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.GridOn,
                            contentDescription = stringResource(R.string.image_sources),
                        )
                    },
                    label = { Text(stringResource(R.string.grid_short)) },
                    selected = selectedNavItem == 2,
                    onClick = { selectedNavItem = 2 },
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    },
                    label = { Text(stringResource(R.string.settings_short)) },
                    selected = false,
                    onClick = onNavigateToSettings,
                )
            }
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
                Spacer(modifier = Modifier.weight(0.45f))
                Text(
                    text = stringResource(R.string.choose_your_preferred_image_source_placeholder,
                        stringResource(R.string.choose_your),
                        stringResource(R.string.preferred_image_source)),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )

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
                            icon = source.icon,
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
