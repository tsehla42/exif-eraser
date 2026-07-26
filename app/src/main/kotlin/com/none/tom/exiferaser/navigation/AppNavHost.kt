package com.none.tom.exiferaser.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.none.tom.exiferaser.ui.screens.help.HelpScreen
import com.none.tom.exiferaser.ui.screens.main.MainScreen
import com.none.tom.exiferaser.ui.screens.metadataViewer.MetadataViewerScreen
import com.none.tom.exiferaser.ui.screens.processing.ProcessingScreen
import com.none.tom.exiferaser.ui.screens.processingDetails.ProcessingDetailsScreen
import com.none.tom.exiferaser.ui.screens.savePath.SavePathScreen
import com.none.tom.exiferaser.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToProcessing = { navController.navigate(Screen.ImageProcessing.route) },
                onNavigateToMetadataViewer = { navController.navigate(Screen.MetadataViewer.route) },
            )
        }
        composable(Screen.ImageProcessing.route) {
            ProcessingScreen(
                onNavigateToDetails = {
                    navController.navigate(Screen.ImageProcessingDetails.route)
                },
                onShareImages = { uris ->
                    // Share intent handled externally
                },
            )
        }
        composable(Screen.ImageProcessingDetails.route) {
            ProcessingDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.SavePathSelection.route) {
            SavePathScreen(
                onNavigateToSelection = { savePath ->
                    navController.popBackStack()
                    navController.navigate(Screen.ImageProcessing.route)
                },
                onDismiss = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.MetadataViewer.route,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            val displayName = backStackEntry.arguments?.getString("displayName") ?: ""
            MetadataViewerScreen(
                onNavigateBack = { navController.popBackStack() },
                uri = Uri.parse(uriString),
                displayName = displayName,
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
