package com.tsehla.exiferaser.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Settings : Screen("settings")
    data object Help : Screen("help")
    data object ImageProcessing : Screen("image_processing/{navArgImageSavePath}") {
        fun createRoute(savePath: String = "") = "image_processing/${Uri.encode(savePath)}"
    }
    data object ImageProcessingDetails : Screen("image_processing_details/{summariesJson}") {
        fun createRoute(summariesJson: String) = "image_processing_details/${Uri.encode(summariesJson)}"
    }
    data object MetadataViewer : Screen("metadata_viewer/{uri}/{displayName}/{hideDelete}") {
        fun createRoute(uri: String, displayName: String, hideDelete: Boolean = false): String {
            return "metadata_viewer/${Uri.encode(uri)}/${Uri.encode(displayName)}/${hideDelete}"
        }
    }
    data object SavePathSelection : Screen("save_path_selection")
}
