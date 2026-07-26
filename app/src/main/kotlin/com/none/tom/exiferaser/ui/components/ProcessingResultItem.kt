package com.none.tom.exiferaser.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.none.tom.exiferaser.core.image.ImageProcessingSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProcessingResultItem(
    summary: ImageProcessingSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = summary.uri,
                contentDescription = summary.displayName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = summary.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                if (summary.extension.isNotEmpty()) {
                    Text(
                        text = summary.extension,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MetadataChips(snapshot = summary.imageMetadataSnapshot)
            }
            Icon(
                imageVector = if (summary.isImageSaved) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
                contentDescription = if (summary.isImageSaved) "Saved" else "Not saved",
                tint = if (summary.isImageSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataChips(
    snapshot: com.none.tom.exiferaser.core.image.ImageMetadataSnapshot,
) {
    val chips = mutableListOf<String>()
    if (snapshot.isExifContained) chips.add("EXIF")
    if (snapshot.isXmpContained) chips.add("XMP")
    if (snapshot.isExtendedXmpContained) chips.add("XMP+")
    if (snapshot.isIccProfileContained) chips.add("ICC")
    if (snapshot.isPhotoshopImageResourcesContained) chips.add("PSD")

    if (chips.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            chips.forEach { label ->
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
    }
}
