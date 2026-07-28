package com.none.tom.exiferaser.ui.screens.processingDetails

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import com.none.tom.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsSideEffect
import com.none.tom.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsViewModel
import com.none.tom.exiferaser.ui.components.ProcessingResultItem

private const val MIME_TYPE_IMAGE = "image/*"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingDetailsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMetadataViewer: (String, String) -> Unit,
    summariesJson: String = "[]",
    viewModel: ImageProcessingDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val summaries = try {
            val jsonArray = org.json.JSONArray(summariesJson)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                com.none.tom.exiferaser.core.image.ImageProcessingSummary(
                    displayName = obj.optString("displayName", ""),
                    extension = obj.optString("extension", ""),
                    mimeType = obj.optString("mimeType", ""),
                    uri = android.net.Uri.parse(obj.optString("uri", "")),
                    isImageSaved = obj.optBoolean("isImageSaved", false),
                    imageMetadataSnapshot = com.none.tom.exiferaser.core.image.ImageMetadataSnapshot(
                        isIccProfileContained = obj.optBoolean("isIccProfileContained", false),
                        isExifContained = obj.optBoolean("isExifContained", false),
                        isPhotoshopImageResourcesContained = obj.optBoolean("isPhotoshopImageResourcesContained", false),
                        isXmpContained = obj.optBoolean("isXmpContained", false),
                        isExtendedXmpContained = obj.optBoolean("isExtendedXmpContained", false),
                    ),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        viewModel.handleImageProcessingSummaries(summaries)
    }

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is ImageProcessingDetailsSideEffect.ViewImage -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndTypeAndNormalize(sideEffect.imageUri, MIME_TYPE_IMAGE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                }
                ImageProcessingDetailsSideEffect.ImageSaved.Unsupported -> {
                    // Could show a toast/snackbar in the future
                }
                is ImageProcessingDetailsSideEffect.Navigate.ToImageModifiedDetails -> {
                    // Could navigate to a details dialog in the future
                }
                is ImageProcessingDetailsSideEffect.Navigate.ToImageSavedDetails -> {
                    // Could show path details in the future
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.processing_details_title)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = state.imageProcessingSummaries,
                key = { "${it.displayName}_${it.extension}" },
            ) { summary ->
                ProcessingResultItemWithExif(
                    summary = summary,
                    onViewImage = { viewModel.handleViewImage(state.imageProcessingSummaries.indexOf(summary)) },
                    onViewExif = { onNavigateToMetadataViewer(summary.uri.toString(), summary.displayName) },
                )
            }
        }
    }
}

@Composable
private fun ProcessingResultItemWithExif(
    summary: ImageProcessingSummary,
    onViewImage: () -> Unit,
    onViewExif: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProcessingResultItem(
            summary = summary,
            onClick = onViewImage,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onViewExif) {
            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = stringResource(R.string.view_new_exif),
            )
        }
    }
}
