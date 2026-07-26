package com.none.tom.exiferaser.ui.screens.processing

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.image.ImageProcessingStep
import com.none.tom.exiferaser.imageProcessing.business.ImageProcessingSideEffect
import com.none.tom.exiferaser.imageProcessing.business.ImageProcessingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    onNavigateToDetails: () -> Unit,
    onShareImages: (List<android.net.Uri>) -> Unit,
    viewModel: ImageProcessingViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val isFinished = state.imageImageProcessingStep is ImageProcessingStep.FinishedBulk

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
                    onNavigateToDetails()
                }
                is ImageProcessingSideEffect.ShareImages -> {
                    onShareImages(sideEffect.imageUris)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.processing)) },
                actions = {
                    if (isFinished && state.imagesSavedCount > 0) {
                        IconButton(onClick = { viewModel.shareImages() }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.share),
                            )
                        }
                    }
                },
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
                    hasSummaries = state.imageProcessingSummaries.isNotEmpty(),
                    onDetailsClick = onNavigateToDetails,
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
        text = stringResource(R.string.images_processed_count, imagesProcessedCount),
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
    hasSummaries: Boolean,
    onDetailsClick: () -> Unit,
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
            text = stringResource(R.string.images_saved_count, imagesSavedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    if (hasSummaries) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.view_details),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp),
        )
    }
}
