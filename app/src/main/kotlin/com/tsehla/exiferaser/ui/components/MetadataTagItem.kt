package com.tsehla.exiferaser.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tsehla.exiferaser.core.extension.copyToClipboard
import com.tsehla.exiferaser.metadataViewer.data.MetadataTag

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetadataTagItem(
    tag: MetadataTag,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    context.copyToClipboard("${tag.tagName}: ${tag.value}", tag.value)
                },
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = tag.tagName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = tag.description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
