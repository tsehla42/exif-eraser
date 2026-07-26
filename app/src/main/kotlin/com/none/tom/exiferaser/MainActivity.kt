package com.none.tom.exiferaser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.navigation.compose.rememberNavController
import com.none.tom.exiferaser.core.util.SETTINGS_KEY_DYNAMIC_COLOR
import com.none.tom.exiferaser.core.util.SETTINGS_KEY_THEME_MODE
import com.none.tom.exiferaser.navigation.AppNavHost
import com.none.tom.exiferaser.settings.business.ThemeMode
import com.none.tom.exiferaser.ui.theme.ExifEraserTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeModeFlow = dataStore.data.map { preferences ->
            ThemeMode.entries.getOrElse(preferences[intPreferencesKey(SETTINGS_KEY_THEME_MODE)] ?: 3) { ThemeMode.SYSTEM }
        }
        val dynamicColorFlow = dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(SETTINGS_KEY_DYNAMIC_COLOR)] ?: true
        }
        setContent {
            val themeMode by themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val useDynamicColor by dynamicColorFlow.collectAsState(initial = true)
            ExifEraserTheme(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
            ) {
                val navController = rememberNavController()
                SideEffect {
                    val window = this@MainActivity.window
                    val isDark = when (themeMode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK, ThemeMode.AMOLED -> true
                        ThemeMode.SYSTEM -> {
                            resources.configuration.uiMode and
                                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                                android.content.res.Configuration.UI_MODE_NIGHT_YES
                        }
                    }
                    val barColor = when {
                        themeMode == ThemeMode.AMOLED -> Color.Black
                        isDark -> Color(0xFF1C1B1F)
                        else -> Color.White
                    }
                    window.navigationBarColor = barColor.toArgb()
                    window.statusBarColor = barColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !isDark
                }
                AppNavHost(navController = navController)
            }
        }
    }
}
