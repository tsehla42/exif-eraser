package com.none.tom.exiferaser.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Settings : Screen("settings")
    data object Help : Screen("help")
    data object ImageProcessing : Screen("image_processing")
    data object ImageProcessingDetails : Screen("image_processing_details/{summariesJson}") {
        fun createRoute(summariesJson: String) = "image_processing_details/${Uri.encode(summariesJson)}"
    }
    data object MetadataViewer : Screen("metadata_viewer/{uri}/{displayName}") {
        fun createRoute(uri: String, displayName: String): String {
            return "metadata_viewer/${Uri.encode(uri)}/${Uri.encode(displayName)}"
        }
    }
    data object SavePathSelection : Screen("save_path_selection")
}
