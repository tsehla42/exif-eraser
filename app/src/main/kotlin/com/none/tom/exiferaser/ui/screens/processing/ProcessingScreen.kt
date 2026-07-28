package com.none.tom.exiferaser.ui.screens.processing

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.image.ImageProcessingStep
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import com.none.tom.exiferaser.core.provider.CameraFileProvider
import com.none.tom.exiferaser.imageProcessing.business.ImageProcessingSideEffect
import com.none.tom.exiferaser.imageProcessing.business.ImageProcessingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    onNavigateToMetadataViewer: (String, String) -> Unit,
    onNavigateToProcessingDetails: (String) -> Unit,
    onShareImages: (List<android.net.Uri>) -> Unit,
    viewModel: ImageProcessingViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val isFinished = state.imageImageProcessingStep is ImageProcessingStep.FinishedBulk
    val context = LocalContext.current

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.processing)) },
            )
        },
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
                    onMetadataViewerClick = onNavigateToMetadataViewer,
                    onNavigateToProcessingDetails = { summaries ->
                        onNavigateToProcessingDetails(serializeSummaries(summaries))
                    },
                    onShareClick = { viewModel.shareImages() },
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
    onMetadataViewerClick: (String, String) -> Unit,
    onNavigateToProcessingDetails: (List<ImageProcessingSummary>) -> Unit,
    onShareClick: () -> Unit,
) {
    Icon(
        imageVector = Icons.Outlined.CheckCircle,
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
    if (imagesSavedCount > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (imagesSavedCount == 1) {
                stringResource(R.string.image_saved)
            } else {
                stringResource(R.string.images_saved_count, imagesSavedCount)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (summaries.isNotEmpty()) {
            val savedPath = summaries.first().uri.path
            if (!savedPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = savedPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
    if (summaries.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        if (summaries.size == 1) {
            TextButton(onClick = {
                val summary = summaries.first()
                onMetadataViewerClick(summary.uri.toString(), summary.displayName)
            }) {
                Text(
                    text = stringResource(R.string.view_new_exif),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            TextButton(onClick = { onNavigateToProcessingDetails(summaries) }) {
                Text(
                    text = stringResource(R.string.view_details),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (imagesSavedCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share),
                )
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.share))
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
