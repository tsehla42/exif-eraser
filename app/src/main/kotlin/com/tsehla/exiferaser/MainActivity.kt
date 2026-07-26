package com.tsehla.exiferaser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.navigation.compose.rememberNavController
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_DYNAMIC_COLOR
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_THEME_MODE
import com.tsehla.exiferaser.main.data.ShareIntentCoordinator
import com.tsehla.exiferaser.main.data.ShareIntentHolder
import com.tsehla.exiferaser.main.data.ShareIntentParser
import com.tsehla.exiferaser.main.data.ShareOutcome
import com.tsehla.exiferaser.navigation.AppNavHost
import com.tsehla.exiferaser.navigation.Screen
import com.tsehla.exiferaser.settings.business.ThemeMode
import com.tsehla.exiferaser.ui.components.ThemedSnackbarHost
import com.tsehla.exiferaser.ui.theme.ExifEraserTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var shareIntentHolder: ShareIntentHolder

    @Inject
    lateinit var shareIntentCoordinator: ShareIntentCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)
        addOnNewIntentListener { newIntent ->
            setIntent(newIntent)
            handleShareIntent(newIntent)
        }
        val themeModeFlow = dataStore.data.map { preferences ->
            ThemeMode.entries.getOrElse(preferences[intPreferencesKey(SETTINGS_KEY_THEME_MODE)] ?: 3) { ThemeMode.SYSTEM }
        }
        val dynamicColorFlow = dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(SETTINGS_KEY_DYNAMIC_COLOR)] ?: true
        }
        setContent {
            val themeMode by themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val useDynamicColor by dynamicColorFlow.collectAsState(initial = true)
            val barColor = remember { mutableStateOf(Color.Black) }
            ExifEraserTheme(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
            ) {
                barColor.value = if (MaterialTheme.colorScheme.background == Color.Black) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                val navController = rememberNavController()
                val shareSnackbarHostState = remember { SnackbarHostState() }
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    shareIntentCoordinator.pendingShare.collect { uris ->
                        if (uris == null) return@collect
                        when (val outcome = shareIntentCoordinator.handlePendingShare(uris)) {
                            ShareOutcome.Unsupported -> {
                                shareSnackbarHostState.showSnackbar(
                                    context.getString(R.string.share_unsupported)
                                )
                            }

                            ShareOutcome.SelectionFailed -> {
                                shareSnackbarHostState.showSnackbar(
                                    context.getString(R.string.image_sources_save_failure)
                                )
                            }

                            is ShareOutcome.NavigateToProcessing -> {
                                navController.navigate(
                                    Screen.ImageProcessing.createRoute(outcome.savePath.toString())
                                )
                            }

                            ShareOutcome.NavigateToSavePathSelection -> {
                                // Keep an already-open save path picker instead of stacking a second one
                                val isSavePathSelectionOpen = navController.currentBackStackEntry
                                    ?.destination?.route == Screen.SavePathSelection.route
                                if (!isSavePathSelectionOpen) {
                                    navController.navigate(Screen.SavePathSelection.route)
                                }
                            }
                        }
                    }
                }
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
                    window.navigationBarColor = barColor.value.toArgb()
                    window.statusBarColor = barColor.value.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !isDark
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(navController = navController)
                    // Share errors can arrive while any tab is open, so they are shown at the
                    // activity level: offset by 80dp (bottom navigation bar) + 16dp gap
                    ThemedSnackbarHost(
                        hostState = shareSnackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 96.dp),
                    )
                }
            }
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val uris = ShareIntentParser.parse(intent) ?: return
        intent.putExtra(ShareIntentParser.EXTRA_SHARE_CONSUMED, true)
        setIntent(intent)
        shareIntentHolder.submit(uris)
    }
}
