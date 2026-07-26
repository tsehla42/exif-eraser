package com.tsehla.exiferaser.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.core.image.ImageMetadataSnapshot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetadataDetailsDialog(
    displayName: String,
    extension: String,
    imageMetadataSnapshot: ImageMetadataSnapshot,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = displayName.ifEmpty {
                    stringResource(R.string.details)
                },
            )
        },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (extension.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = extension.uppercase())
                        },
                    )
                }
                if (imageMetadataSnapshot.isIccProfileContained) {
                    MetadataChip(label = stringResource(R.string.icc_profile))
                }
                if (imageMetadataSnapshot.isExifContained) {
                    MetadataChip(label = stringResource(R.string.exif))
                }
                if (imageMetadataSnapshot.isXmpContained) {
                    MetadataChip(label = stringResource(R.string.xmp))
                }
                if (imageMetadataSnapshot.isExtendedXmpContained) {
                    MetadataChip(label = stringResource(R.string.extended_xmp))
                }
                if (imageMetadataSnapshot.isPhotoshopImageResourcesContained) {
                    MetadataChip(label = stringResource(R.string.photoshop))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
private fun MetadataChip(label: String, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = {
            Text(text = label)
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}
