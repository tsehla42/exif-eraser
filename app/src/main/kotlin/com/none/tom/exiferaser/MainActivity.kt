package com.none.tom.exiferaser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.none.tom.exiferaser.navigation.AppNavHost
import com.none.tom.exiferaser.ui.theme.ExifEraserTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExifEraserTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
