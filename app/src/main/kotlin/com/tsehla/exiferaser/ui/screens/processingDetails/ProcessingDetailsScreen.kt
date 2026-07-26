package com.tsehla.exiferaser.ui.screens.processingDetails

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.core.extension.copyToClipboard
import com.tsehla.exiferaser.core.image.ImageProcessingSummary
import com.tsehla.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsSideEffect
import com.tsehla.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsViewModel

private const val MIME_TYPE_IMAGE = "image/*"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingDetailsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
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
                com.tsehla.exiferaser.core.image.ImageProcessingSummary(
                    displayName = obj.optString("displayName", ""),
                    extension = obj.optString("extension", ""),
                    mimeType = obj.optString("mimeType", ""),
                    uri = android.net.Uri.parse(obj.optString("uri", "")),
                    isImageSaved = obj.optBoolean("isImageSaved", false),
                    imageMetadataSnapshot = com.tsehla.exiferaser.core.image.ImageMetadataSnapshot(
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
                is ImageProcessingDetailsSideEffect.ShareImage -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = MIME_TYPE_IMAGE
                        putExtra(Intent.EXTRA_STREAM, sideEffect.imageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                }
                is ImageProcessingDetailsSideEffect.CopyPath -> {
                    context.copyToClipboard("image path", sideEffect.path)
                }
                ImageProcessingDetailsSideEffect.ImageSaved.Unsupported -> {
                }
                is ImageProcessingDetailsSideEffect.Navigate.ToImageModifiedDetails -> {
                }
                is ImageProcessingDetailsSideEffect.Navigate.ToImageSavedDetails -> {
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.processing_details_title)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = state.imageProcessingSummaries,
                    key = { "${it.uri}_${it.displayName}_${it.extension}" },
                ) { summary ->
                    val index = state.imageProcessingSummaries.indexOf(summary)
                    ProcessingResultItemSimple(
                        summary = summary,
                        onViewExif = {
                            onNavigateToMetadataViewer(
                                summary.uri.toString(),
                                summary.displayName
                            )
                        },
                        onOpenWith = {
                            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndTypeAndNormalize(summary.uri, MIME_TYPE_IMAGE)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    openIntent,
                                    context.getString(R.string.open_with)
                                )
                            )
                        },
                        onCopyPath = { viewModel.handleCopyPath(index) },
                        onShare = { viewModel.handleShareImage(index) },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(onClick = onNavigateToMain) {
                    Text(text = stringResource(R.string.main_short))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessingResultItemSimple(
    summary: ImageProcessingSummary,
    onViewExif: () -> Unit,
    onOpenWith: () -> Unit,
    onCopyPath: () -> Unit,
    onShare: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val menuShape = RoundedCornerShape(16.dp)
    val menuContainerColor = Color(0xFF111111)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMenu = true },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = summary.uri,
                contentDescription = summary.displayName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = com.tsehla.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsViewModel.getDisplayPath(summary.uri),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = menuShape,
            containerColor = menuContainerColor,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.view_new_exif)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_visibility_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    onViewExif()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.open_with)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_open_in_new_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    onOpenWith()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy_path)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_content_copy_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    onCopyPath()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share_image)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_share_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    onShare()
                },
            )
        }
    }
}
