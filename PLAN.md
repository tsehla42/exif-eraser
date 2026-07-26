# ExifEraser — New Compose-Based App

## Overview

Fresh Android app built with Jetpack Compose that:
1. **Removes EXIF metadata** from images (ported from Tommy's exif-eraser)
2. **Displays full EXIF metadata** (new feature using `metadata-extractor`)

## What's Already Done

48 files of business logic copied from Tommy's project:
- 5 core image models (ImageFormat, ImageMetadataSnapshot, etc.)
- 5 repositories (ImageProcessing, Settings, Main, Selection, ImageSources)
- 17 ViewModel/State/SideEffect files (Orbit MVI)
- 3 DI modules (Hilt)
- 5 core extensions, 3 utilities, 2 serializers, 3 contracts, 1 provider
- 2 proto files, 1 Application class, 1 Manifest
- **Total: 4,111 lines of reusable business logic**

## What Needs to Be Built

### Phase 1: Project Scaffolding

#### 1.1 Build Configuration
- `build.gradle.kts` (project-level) — AGP, Kotlin, Compose compiler plugins
- `app/build.gradle.kts` — dependencies: Compose BOM, Material3, Navigation Compose, Hilt Compose, Coil Compose, metadata-extractor, orbit-mvi, datastore, wire
- `gradle/libs.versions.toml` — version catalog
- `settings.gradle.kts` — single module
- `gradle.properties` — AndroidX, non-transitive R classes
- `gradle/wrapper/` — copy from Tommy's project

#### 1.2 Theme & Design System
- `ui/theme/Color.kt` — Material3 color scheme
- `ui/theme/Theme.kt` — ExifEraserTheme composable
- `ui/theme/Type.kt` — Typography

#### 1.3 Application Entry Point
- Modify `ExifEraserApplication.kt` if needed (currently copied as-is)
- `MainActivity.kt` — `ComponentActivity` with `setContent { ExifEraserTheme { AppNavHost() } }`

### Phase 2: Core Navigation

#### 2.1 Navigation Graph
- `navigation/Screen.kt` — sealed class of routes (Main, Settings, Help, ImageProcessing, ProcessingDetails, MetadataViewer)
- `navigation/AppNavHost.kt` — Compose `NavHost` with all destinations

#### 2.2 Main Screen (Home)
- `ui/screens/main/MainScreen.kt` — image source selection grid, FAB, toolbar
- `ui/components/ImageSourceCard.kt` — composable for each source type (file, files, directory, camera)
- Handles: pick images, pick directory, camera, paste, drag-and-drop, share intent

### Phase 3: Image Processing Flow

#### 3.1 Save Path Selection
- `ui/screens/savePath/SavePathScreen.kt` — bottom sheet or dialog for choosing output directory

#### 3.2 Processing
- `ui/screens/processing/ProcessingScreen.kt` — progress indicator, image preview, status text
- Observes `ImageProcessingViewModel` state flow

#### 3.3 Processing Details
- `ui/screens/processingDetails/ProcessingDetailsScreen.kt` — list of processed images with results
- `ui/components/ProcessingResultItem.kt` — composable for each result row

### Phase 4: Settings

- `ui/screens/settings/SettingsScreen.kt` — preference list using Compose
- `ui/components/PreferenceItem.kt` — reusable composable for clickable/switch/text preferences
- Settings: night mode, default save path, display name suffix, skip save path selection, legacy image selection

### Phase 5: Help

- `ui/screens/help/HelpScreen.kt` — help items list
- `ui/components/HelpItem.kt` — composable for each help entry

### Phase 6: Metadata Viewer (NEW FEATURE)

#### 6.1 Dependency
Add to `gradle/libs.versions.toml`:
```toml
metadata-extractor = "2.21.1"
```

#### 6.2 Data Layer
- `metadataViewer/data/MetadataViewerRepository.kt` — reads metadata via `MetadataReader.readMetadata(inputStream)`, groups by Directory, returns `List<MetadataGroup>`

#### 6.3 Business Layer
- `metadataViewer/business/MetadataViewerState.kt` — state with `List<MetadataGroup>`, loading, error
- `metadataViewer/business/MetadataViewerSideEffect.kt` — navigation events
- `metadataViewer/business/MetadataViewerViewModel.kt` — Orbit MVI ViewModel

#### 6.4 UI
- `ui/screens/metadataViewer/MetadataViewerScreen.kt` — expandable list of metadata groups
- `ui/components/MetadataGroupHeader.kt` — group name (EXIF, XMP, IPTC) with expand/collapse
- `ui/components/MetadataTagItem.kt` — tag name + value row
- Entry point: long-press on image or "View metadata" button in processing results

### Phase 7: Dialogs

- `ui/components/DeleteConfirmationDialog.kt` — replaces DeleteCameraImagesFragment
- `ui/components/NightModePickerDialog.kt` — replaces DefaultNightModeFragment
- `ui/components/DisplayNameSuffixDialog.kt` — replaces DefaultDisplayNameSuffixFragment
- `ui/components/MetadataDetailsDialog.kt` — replaces ImageMetadataDetailsFragment

### Phase 8: Shared Components

- `ui/components/AdaptiveScaffold.kt` — responsive layout using WindowSizeClass
- `ui/components/AnimatedNavTransition.kt` — MaterialSharedAxis equivalents

## File Structure (Target)

```
app/src/main/kotlin/com/none/tom/exiferaser/
├── ExifEraserApplication.kt          # copied
├── MainActivity.kt                   # NEW
├── core/
│   ├── contract/                     # copied (3 files)
│   ├── di/                           # copied (3 files)
│   ├── extension/                    # copied (5 files)
│   ├── image/                        # copied (5 files)
│   ├── provider/                     # copied (1 file)
│   ├── serializer/                   # copied (2 files)
│   └── util/                         # copied (3 files)
├── main/
│   ├── business/                     # copied (3 files)
│   └── data/                         # copied (3 files)
├── imageProcessing/
│   ├── business/                     # copied (3 files)
│   └── data/                         # copied (1 file)
├── imageProcessingDetails/
│   └── business/                     # copied (3 files)
├── imageSavePathSelection/
│   └── business/                     # copied (3 files)
├── imageDetails/
│   └── business/                     # copied (2 files)
├── settings/
│   ├── business/                     # copied (3 files)
│   └── data/                         # copied (1 file)
├── metadataViewer/                   # ALL NEW
│   ├── business/                     # 3 files
│   └── data/                         # 1 file
├── navigation/                       # NEW
│   ├── Screen.kt
│   └── AppNavHost.kt
└── ui/                               # ALL NEW
    ├── theme/                        # 3 files
    ├── screens/                      # 8 screen composables
    └── components/                   # 12 reusable composables
```

## Dependencies

```toml
[versions]
# Compose BOM
compose-bom = "2025.06.00"
# Existing from Tommy's project
activity = "1.13.0"
agp = "9.2.1"
apache-commons-io = "2.22.0"
coil = "3.1.0"                        # upgrade to Compose version
core = "1.19.0"
coroutines = "1.11.0"
dagger-hilt = "2.60"
datastore = "1.2.1"
exif-interface = "1.1.0"
metadata-extractor = "2.21.1"         # NEW
navigation = "2.9.8"
orbit = "11.0.0"
timber = "5.0.1"
wire = "6.4.5"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.11.0" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }

# Existing
metadata-extractor = { module = "com.drewnoakes:metadata-extractor", version.ref = "metadata-extractor" }
exif-interface = { module = "io.github.tommy-geenexus:exif-interface-extended", version.ref = "exif-interface" }
orbit-vm = { module = "org.orbit-mvi:orbit-viewmodel", version.ref = "orbit" }
dagger-hilt = { module = "com.google.dagger:hilt-android", version.ref = "dagger-hilt" }
dagger-hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "dagger-hilt" }
timber = { module = "com.jakewharton.timber:timber", version.ref = "timber" }
wire = { module = "com.squareup.wire:wire-runtime", version.ref = "wire" }
```

## Implementation Order

| Phase | What | Est. Time | Depends On |
|-------|------|-----------|------------|
| 1 | Project scaffolding (build files, theme, activity) | 1 day | Nothing |
| 2 | Navigation + Main screen | 1 day | Phase 1 |
| 3 | Image processing flow | 1.5 days | Phase 2 |
| 4 | Settings screen | 0.5 days | Phase 2 |
| 5 | Help screen | 0.25 days | Phase 2 |
| 6 | Metadata viewer (NEW) | 2 days | Phase 2 + metadata-extractor |
| 7 | Dialogs | 0.5 days | Phase 2 |
| 8 | Shared components (adaptive layout, transitions) | 1 day | Phase 2 |
| 9 | Polish, edge-to-edge, drag-and-drop | 1.5 days | All above |
| 10 | Testing | 1.5 days | All above |
| **Total** | | **~10 days** | |

## Key Decisions

1. **Keep Orbit MVI** — it works well with Compose and the ViewModels are already written
2. **Keep package structure** — same `core/`, `main/`, `imageProcessing/`, etc. structure
3. **Coil 3** — use coil3 (Compose-native) instead of coil 2
4. **Material3** — full Material Design 3 with dynamic color support
5. **Single Activity** — `MainActivity` with Compose NavHost, no Fragments
6. **Edge-to-edge** — Compose `Modifier.windowInsetsPadding()` instead of manual ViewCompat calls
