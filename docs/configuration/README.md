# Configuration

## Build Configuration

| Property | Value |
|---|---|
| ApplicationId | `com.tsehla.exiferaser` |
| Namespace | `com.none.tom.exiferaser` |
| Compile SDK | 37 |
| Min SDK | 28 |
| Target SDK | 37 |
| Version Code | 45 |
| Version Name | 1.0.0 |
| Java Target | 17 |
| Kotlin Compose | 2.4.0 |
| AGP | 9.2.1 |
| R8/ProGuard | Enabled (release) |
| Compose | Enabled |
| BuildConfig | Enabled |

## Dependencies

### Compose BOM (2026.06.01)
- `androidx.compose.ui:ui`
- `androidx.compose.ui:ui-graphics`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.compose.material3:material3`
- `androidx.compose.material:material-icons-extended`
- `androidx.compose.runtime:runtime`

### AndroidX
- `activity-compose:1.13.0`
- `appcompat:1.7.0`
- `core-ktx:1.19.0`
- `documentfile:1.1.0`
- `fragment-ktx:1.8.6`
- `lifecycle-runtime-compose:2.11.0`
- `lifecycle-viewmodel-compose:2.11.0`
- `navigation-compose:2.9.8`
- `navigation-fragment-ktx:2.9.8`
- `navigation-ui-ktx:2.9.8`
- `window:1.5.1`
- `window-core:1.5.1`

### Hilt (2.60)
- `hilt-android`
- `hilt-android-compiler` (KSP)
- `hilt-navigation-compose:1.2.0`

### DataStore (1.2.1)
- `datastore-preferences`
- `datastore-core`

### Media / Metadata
- `exif-interface-extended:1.1.0` (Tommy's fork)
- `metadata-extractor:2.21.0` (Drew Noakes)
- `coil-compose:3.1.0`

### Other
- `orbit-viewmodel:11.0.0`
- `wire-runtime:6.4.5`
- `timber:5.0.1`
- `commons-io:2.22.0`
- `material:1.12.0` (Material Components)
- `kotlinx-coroutines-android:1.11.0`

## Theme System

### ThemeMode Enum

Defined in `settings/business/SettingsState.kt`:

```kotlin
enum class ThemeMode {
    LIGHT,   // Always light
    DARK,    // Always dark
    AMOLED,  // Dark + pure black backgrounds
    SYSTEM,  // Follows system dark theme setting
}
```

### Dynamic Colors

- Enabled by default (`useDynamicColor = true`)
- Material You support on Android 12+ (`Build.VERSION_CODES.S`)
- Uses `dynamicDarkColorScheme()` / `dynamicLightColorScheme()`
- Falls back to static color schemes on older devices
- Togglable in Settings

### AMOLED Mode

When `ThemeMode.AMOLED` is selected:
- Uses dark base scheme (dynamic or static)
- Overrides all surface/background colors to `Color.Black`
- `surfaceContainer` uses `Color(0xFF202020)` for subtle contrast
- Navigation and status bars set to black in `MainActivity`
- Light navigation bar appearance disabled

### Color Schemes

- `LightColorScheme` - Full Material3 light palette
- `DarkColorScheme` - Full Material3 dark palette
- AMOLED - Dark scheme with black overrides applied via `copy()`

### Settings Persistence

Theme preferences stored in DataStore:
- `theme_mode` (int) - Ordinal of `ThemeMode` enum
- `dynamic_color` (boolean) - Whether to use Material You colors

`MainActivity` collects these flows to apply theme at the top level:
```kotlin
val themeModeFlow = dataStore.data.map { ... ThemeMode.entries.getOrElse(...) }
val dynamicColorFlow = dataStore.data.map { ... }
```

## Launcher Icon

Adaptive icon with Material You dynamic color support (Android 12+).

### Structure

| File | Purpose |
|------|---------|
| `drawable/ic_launcher_foreground.xml` | Foreground vector (icon shape) |
| `drawable/ic_launcher_monochrome.xml` | Monochrome layer for themed icons |
| `mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon definition (bg + fg + mono) |
| `values/colors.xml` | Light mode colors |
| `values-night/colors.xml` | Dark mode colors |
| `values-v31/colors.xml` | Android 12+ dynamic colors |
| `values-night-v31/colors.xml` | Android 12+ dark dynamic colors |

### Color Scheme

Uses `ic_launcher_background` and `ic_launcher_foreground` color resources with 4 qualifier variants:

| Mode | Background | Foreground |
|------|-----------|------------|
| Light (pre-12) | `#ABE87C` | `#394F29` |
| Dark (pre-12) | `#2A3025` | `#ABE87C` |
| Light (12+) | `system_accent1_100` | `system_accent1_700` |
| Dark (12+) | `system_accent2_800` | `system_accent1_100` |

On Android 12+, the system wallpaper-derived palette automatically colors the icon. The monochrome layer enables themed icon support when enabled in device settings.
