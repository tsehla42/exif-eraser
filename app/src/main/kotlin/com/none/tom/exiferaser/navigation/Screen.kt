package com.none.tom.exiferaser.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Settings : Screen("settings")
    data object Help : Screen("help")
    data object ImageProcessing : Screen("image_processing")
    data object ImageProcessingDetails : Screen("image_processing_details")
    data object MetadataViewer : Screen("metadata_viewer/{uri}/{displayName}") {
        fun createRoute(uri: Uri, displayName: String): String {
            return "metadata_viewer/${Uri.encode(uri.toString())}/${Uri.encode(displayName)}"
        }
    }
    data object SavePathSelection : Screen("save_path_selection")
}
