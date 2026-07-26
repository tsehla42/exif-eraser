package com.tsehla.exiferaser.ui.screens.processing

import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.core.image.ImageProcessingStep
import com.tsehla.exiferaser.core.image.ImageProcessingSummary
import com.tsehla.exiferaser.core.provider.CameraFileProvider
import com.tsehla.exiferaser.imageProcessing.business.ImageProcessingSideEffect
import com.tsehla.exiferaser.imageProcessing.business.ImageProcessingViewModel
import com.tsehla.exiferaser.ui.components.ShareSelectionDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    onNavigateToMetadataViewer: (String, String) -> Unit,
    onNavigateToProcessingDetails: (String) -> Unit,
    onShareImages: (List<android.net.Uri>) -> Unit,
    onClose: () -> Unit,
    viewModel: ImageProcessingViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val isFinished = state.imageImageProcessingStep is ImageProcessingStep.FinishedBulk
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pickDestinationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { viewModel.saveElsewhere(it) }
    }

    val mediaDeletionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        viewModel.handleDeletionRequestResult()
    }

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is ImageProcessingSideEffect.Handle.UserImagesSelection -> {
                    viewModel.handleUserImagesSelection(sideEffect.protos, sideEffect.treeUri)
                }
                ImageProcessingSideEffect.Handle.UnsupportedSelection -> {
                    viewModel.handleUnsupportedSelection()
                }
                is ImageProcessingSideEffect.Navigate.ToImageProcessingDetails -> {
                    onNavigateToProcessingDetails(
                        serializeSummaries(sideEffect.imageProcessingSummaries.toList())
                    )
                }
                ImageProcessingSideEffect.Navigate.Close -> {
                    onClose()
                }
                is ImageProcessingSideEffect.RequestMediaDeletion -> {
                    mediaDeletionLauncher.launch(
                        IntentSenderRequest.Builder(sideEffect.pendingIntent.intentSender).build()
                    )
                }
                ImageProcessingSideEffect.OriginalsDeletionFailed -> {
                    launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.originals_delete_failed)
                        )
                    }
                }
                is ImageProcessingSideEffect.ShareImages -> {
                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "image/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(sideEffect.imageUris))
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                }
            }
        }
    }

    if (state.isShareDialogVisible) {
        ShareSelectionDialog(
            title = stringResource(R.string.some_files_already_no_exif),
            message = stringResource(R.string.share_dialog_message),
            shareAllText = stringResource(R.string.share_all_files),
            shareConvertedText = stringResource(R.string.share_only_converted),
            onShareAll = { viewModel.shareImagesFromDialog(convertedOnly = false) },
            onShareConverted = { viewModel.shareImagesFromDialog(convertedOnly = true) },
            onCancel = { viewModel.dismissShareDialog() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.processing)) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isFinished) {
                FinishedContent(
                    imagesWithMetadataCount = state.imagesWithMetadataCount,
                    imagesProcessedCount = state.imagesProcessedCount,
                    imagesSavedCount = state.imagesSavedCount,
                    summaries = state.imageProcessingSummaries,
                    isDeleteConvertedChecked = state.isDeleteConvertedChecked,
                    isDeleteOriginalsChecked = state.isDeleteOriginalsChecked,
                    onNavigateToProcessingDetails = { summaries ->
                        onNavigateToProcessingDetails(serializeSummaries(summaries))
                    },
                    onShareClick = { viewModel.shareImages() },
                    onToggleDeleteConverted = { viewModel.toggleDeleteConverted() },
                    onToggleDeleteOriginals = { viewModel.toggleDeleteOriginals() },
                    onSaveElsewhere = { pickDestinationLauncher.launch(null) },
                    onConfirm = { viewModel.handleConfirmClose() },
                )
            } else {
                ProgressContent(
                    progressDisplayValue = state.progress.displayValue,
                    imagesProcessedCount = state.imagesProcessedCount,
                )
            }
        }
    }
}

@Composable
private fun ProgressContent(
    progressDisplayValue: Int,
    imagesProcessedCount: Int,
) {
    Text(
        text = stringResource(R.string.progress_percent, progressDisplayValue),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    LinearProgressIndicator(
        progress = { progressDisplayValue / 100f },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = if (imagesProcessedCount == 1) {
            stringResource(R.string.image_processed)
        } else {
            stringResource(R.string.images_processed_count, imagesProcessedCount)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FinishedContent(
    imagesWithMetadataCount: Int,
    imagesProcessedCount: Int,
    imagesSavedCount: Int,
    summaries: List<ImageProcessingSummary>,
    isDeleteConvertedChecked: Boolean,
    isDeleteOriginalsChecked: Boolean,
    onNavigateToProcessingDetails: (List<ImageProcessingSummary>) -> Unit,
    onShareClick: () -> Unit,
    onToggleDeleteConverted: () -> Unit,
    onToggleDeleteOriginals: () -> Unit,
    onSaveElsewhere: () -> Unit,
    onConfirm: () -> Unit,
) {
    Icon(
        painter = painterResource(R.drawable.baseline_check_circle_24),
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.processing_complete),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    val showSavedCount = imagesSavedCount > 0 && imagesWithMetadataCount != imagesProcessedCount
    if (showSavedCount) {
        Text(
            text = stringResource(R.string.saved_images_count, imagesSavedCount),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
    
    Text(
        text = stringResource(
            R.string.images_modified_placeholder,
            imagesWithMetadataCount,
            stringResource(R.string.of),
            imagesProcessedCount,
            stringResource(R.string.images_modified),
        ),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    
    val alreadyNoExifCount = imagesProcessedCount - imagesWithMetadataCount
    if (alreadyNoExifCount > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (imagesWithMetadataCount == 0) {
                stringResource(R.string.files_already_no_exif)
            } else {
                stringResource(R.string.some_files_already_no_exif)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
    if (summaries.isNotEmpty()) {
        Spacer(modifier = Modifier.height(40.dp))
        TextButton(onClick = { onNavigateToProcessingDetails(summaries) }) {
            Text(
                text = stringResource(R.string.view_details),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (summaries.isNotEmpty()) {
                OutlinedButton(onClick = onShareClick) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_share_24),
                        contentDescription = stringResource(R.string.share),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.share))
                }
            }
            OutlinedButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.ok))
            }
        }

        if (imagesSavedCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onSaveElsewhere) {
                Icon(
                    painter = painterResource(R.drawable.baseline_save_24),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.save_elsewhere))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.delete_files_label),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.originals_label),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Checkbox(
                    checked = isDeleteOriginalsChecked,
                    onCheckedChange = { onToggleDeleteOriginals() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.converted_label),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Checkbox(
                    checked = isDeleteConvertedChecked,
                    onCheckedChange = { onToggleDeleteConverted() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

private fun serializeSummaries(summaries: List<ImageProcessingSummary>): String {
    val jsonArray = org.json.JSONArray()
    summaries.forEach { summary ->
        jsonArray.put(org.json.JSONObject().apply {
            put("displayName", summary.displayName)
            put("extension", summary.extension)
            put("mimeType", summary.mimeType)
            put("uri", summary.uri.toString())
            put("isImageSaved", summary.isImageSaved)
            put("isIccProfileContained", summary.imageMetadataSnapshot.isIccProfileContained)
            put("isExifContained", summary.imageMetadataSnapshot.isExifContained)
            put("isPhotoshopImageResourcesContained", summary.imageMetadataSnapshot.isPhotoshopImageResourcesContained)
            put("isXmpContained", summary.imageMetadataSnapshot.isXmpContained)
            put("isExtendedXmpContained", summary.imageMetadataSnapshot.isExtendedXmpContained)
        })
    }
    return jsonArray.toString()
}
