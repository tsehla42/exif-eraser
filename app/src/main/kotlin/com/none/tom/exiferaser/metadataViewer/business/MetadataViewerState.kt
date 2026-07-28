package com.none.tom.exiferaser.metadataViewer.business

import android.net.Uri
import android.os.Parcelable
import com.none.tom.exiferaser.metadataViewer.data.MetadataGroup
import kotlinx.parcelize.Parcelize

@Parcelize
data class MetadataViewerState(
    val displayName: String = "",
    val imageUri: Uri = Uri.EMPTY,
    val metadataGroups: List<MetadataGroup> = emptyList(),
    val expandedGroups: Set<String> = emptySet(),
    val isRawMode: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null,
) : Parcelable
