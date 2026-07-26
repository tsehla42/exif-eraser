package com.tsehla.exiferaser.metadataViewer.business

import android.net.Uri

sealed class MetadataViewerSideEffect {
    data class Error(val message: String) : MetadataViewerSideEffect()
    data class NavigateToProcessing(val savePath: Uri) : MetadataViewerSideEffect()
    data object NavigateToSavePathSelection : MetadataViewerSideEffect()
}
