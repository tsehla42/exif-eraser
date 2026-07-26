# Themes

## Overview

ExifEraser supports four theme modes with Material You dynamic color and AMOLED pure black support.

## ThemeMode Enum

Defined in `settings/business/SettingsState.kt`:

```kotlin
enum class ThemeMode {
    LIGHT,   // Always light theme
    DARK,    // Always dark theme
    AMOLED,  // Dark theme with pure black backgrounds
    SYSTEM,  // Follows system dark/light setting
}
```

Default: `ThemeMode.SYSTEM`

## How Themes Work

### Theme Selection

`ExifEraserTheme` composable in `ui/theme/Theme.kt`:
1. Determines dark/light based on `ThemeMode`
2. Selects base color scheme:
   - Android 12+ with dynamic color enabled → `dynamicDarkColorScheme()` / `dynamicLightColorScheme()`
   - Otherwise → `DarkColorScheme` or `LightColorScheme`
3. If AMOLED mode → overlays pure black on the base scheme

### Dynamic Colors

- Enabled by default (`useDynamicColor = true`)
- Only available on Android 12+ (`Build.VERSION_CODES.S`)
- Uses device wallpaper colors for Material You theming
- Togglable in Settings screen
- Falls back to static color schemes when disabled or on older devices

### AMOLED Implementation

When `ThemeMode.AMOLED`:
1. Starts with dark base scheme (dynamic or static)
2. Overrides these colors via `copy()`:
   - `background = Color.Black`
   - `surface = Color.Black`
   - `surfaceVariant = Color.Black`
   - `surfaceContainerLowest = Color.Black`
   - `surfaceContainerLow = Color.Black`
   - `surfaceContainer = Color(0xFF202020)` (subtle contrast)
   - `surfaceContainerHigh = Color.Black`
   - `surfaceContainerHighest = Color.Black`
   - `surfaceDim = Color.Black`
   - `surfaceBright = Color.Black`
   - `onBackground = Color.White`
   - `onSurface = Color.White`
3. `MainActivity` sets navigation/status bars to black
4. Light navigation bar appearance disabled

### System Bars

`MainActivity` handles edge-to-edge and bar colors:
- AMOLED → black bars
- Other themes → transparent bars
- `WindowCompat.getInsetsController` adjusts light/dark bar appearance

## Settings Persistence

Stored in DataStore:
- `theme_mode` (int) - Ordinal of `ThemeMode` (0=LIGHT, 1=DARK, 2=AMOLED, 3=SYSTEM)
- `dynamic_color` (boolean) - Whether to use dynamic colors

`MainActivity` collects these as flows and applies at the top level.

## Color Palettes

Defined in `ui/theme/Color.kt`:
- `md_theme_light_*` - Light palette colors
- `md_theme_dark_*` - Dark palette colors

Applied via `LightColorScheme` and `DarkColorScheme` in `ui/theme/Theme.kt`.

## Typography

Defined in `ui/theme/Type.kt` using Material3 `Typography`.
