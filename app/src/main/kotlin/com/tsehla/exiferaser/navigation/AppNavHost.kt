package com.tsehla.exiferaser.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tsehla.exiferaser.ui.screens.help.HelpScreen
import com.tsehla.exiferaser.ui.screens.main.MainScreen
import com.tsehla.exiferaser.ui.screens.metadataViewer.MetadataViewerScreen
import com.tsehla.exiferaser.ui.screens.processing.ProcessingScreen
import com.tsehla.exiferaser.ui.screens.processingDetails.ProcessingDetailsScreen
import com.tsehla.exiferaser.ui.screens.savePath.SavePathScreen
import com.tsehla.exiferaser.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToSavePathSelection = { navController.navigate(Screen.SavePathSelection.route) },
                onNavigateToProcessing = { savePath ->
                    navController.navigate(Screen.ImageProcessing.createRoute(savePath))
                },
                onNavigateToMetadataViewer = { uri, displayName ->
                    navController.navigate(Screen.MetadataViewer.createRoute(uri, displayName))
                },
            )
        }
        composable(
            route = Screen.ImageProcessing.route,
            arguments = listOf(
                navArgument("navArgImageSavePath") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            ProcessingScreen(
                onNavigateToMetadataViewer = { uri, displayName ->
                    navController.navigate(Screen.MetadataViewer.createRoute(uri, displayName))
                },
                onNavigateToProcessingDetails = { summariesJson ->
                    navController.navigate(Screen.ImageProcessingDetails.createRoute(summariesJson))
                },
                onShareImages = { uris ->
                    // Share intent handled externally
                },
                onClose = {
                    // Return to whichever screen opened the conversion flow
                    // (tools, help or settings tab)
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = Screen.ImageProcessingDetails.route,
            arguments = listOf(
                navArgument("summariesJson") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val summariesJson = backStackEntry.arguments?.getString("summariesJson") ?: "[]"
            ProcessingDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToMetadataViewer = { uri, displayName ->
                    navController.navigate(Screen.MetadataViewer.createRoute(uri, displayName, hideDelete = true))
                },
                summariesJson = summariesJson,
            )
        }
        composable(Screen.SavePathSelection.route) {
            SavePathScreen(
                onNavigateToSelection = { savePath ->
                    navController.popBackStack()
                    navController.navigate(Screen.ImageProcessing.createRoute(savePath.toString()))
                },
                onDismiss = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.MetadataViewer.route,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType },
                navArgument("hideDelete") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            val displayName = backStackEntry.arguments?.getString("displayName") ?: ""
            val hideDelete = backStackEntry.arguments?.getBoolean("hideDelete") ?: false
            MetadataViewerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProcessing = { savePath ->
                    navController.navigate(Screen.ImageProcessing.createRoute(savePath))
                },
                onNavigateToSavePathSelection = {
                    navController.navigate(Screen.SavePathSelection.route)
                },
                uri = Uri.parse(uriString),
                displayName = displayName,
                hideDeleteButton = hideDelete,
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) {
                            inclusive = true
                        }
                    }
                },
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) {
                            inclusive = true
                        }
                    }
                },
            )
        }
    }
}
