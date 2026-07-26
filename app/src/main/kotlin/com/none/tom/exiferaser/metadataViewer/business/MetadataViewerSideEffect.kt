package com.none.tom.exiferaser.metadataViewer.business

sealed class MetadataViewerSideEffect {
    data class Error(val message: String) : MetadataViewerSideEffect()
}
