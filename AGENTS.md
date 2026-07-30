# Agents Guide

Entry point for AI agents working on this project. Start here to understand the codebase before making changes.

## What This Project Is

A Jetpack Compose Android app that removes EXIF metadata from images and displays full EXIF/metadata information. Ported from Tommy's exif-eraser with added metadata viewer feature.

**Tech stack:** Kotlin, Jetpack Compose, Material3, Hilt (DI), Orbit MVI, Navigation Compose, metadata-extractor, exif-interface-extended, DataStore, Wire (protobuf)

**Upstream reference:** This project is a spin-off from [Tommy-Geenexus/exif-eraser](https://github.com/Tommy-Geenexus/exif-eraser). That repository is used as a reference for building this one — check it for original implementations, patterns, and design decisions when working on this codebase.

## Documentation

For detailed documentation, see `docs/README.md` (human-readable overview) and the following module docs:

- `docs/architecture/` - Module responsibilities and data flow
- `docs/configuration/` - Build config, dependencies, themes
- `docs/features/` - Feature details (EXIF removal, viewer, themes)
- `docs/guides/` - Development setup and contributing

## Project Structure

```
exif-eraser/
├── app/src/main/kotlin/com/none/tom/exiferaser/
│   ├── ExifEraserApplication.kt    # Hilt app entry, dynamic colors, Timber init
│   ├── MainActivity.kt             # Single activity, edge-to-edge, theme collection
│   ├── core/                       # Shared utilities
│   │   ├── contract/               # ActivityResultContracts (share, camera, view URL)
│   │   ├── di/                     # Hilt modules (Context, Coroutine, DataStore)
│   │   ├── extension/              # Extension functions (ClipData, Context, URI, ContentResolver, etc.)
│   │   ├── image/                  # Image models (format, metadata snapshot, processing)
│   │   ├── provider/               # CameraFileProvider for camera capture
│   │   ├── serializer/             # Wire protobuf serializers
│   │   └── util/                   # Constants, file utils
│   ├── main/                       # Main screen feature
│   │   ├── business/               # MainViewModel, MainState, MainSideEffect
│   │   └── data/                   # MainRepository, SelectionRepository, ImageSourcesRepository
│   ├── imageProcessing/            # EXIF removal feature
│   │   ├── business/               # ImageProcessingViewModel, State, SideEffect
│   │   └── data/                   # ImageProcessingRepository
│   ├── imageProcessingDetails/     # Processing results detail screen
│   │   └── business/               # DetailsViewModel, State, SideEffect
│   ├── imageSavePathSelection/     # Save path picker
│   │   └── business/               # SavePathViewModel, State, SideEffect
│   ├── imageDetails/               # Single image metadata details
│   │   └── business/               # DetailsViewModel, State
│   ├── metadataViewer/             # Full metadata viewer feature
│   │   ├── business/               # MetadataViewerViewModel, State, SideEffect
│   │   └── data/                   # MetadataViewerRepository
│   ├── settings/                   # Settings feature
│   │   ├── business/               # SettingsViewModel, SettingsState, ThemeMode
│   │   └── data/                   # SettingsRepository (DataStore)
│   ├── navigation/                 # Navigation graph and Screen routes
│   └── ui/                         # Compose UI
│       ├── components/             # Reusable composables (dialogs, cards, metadata items)
│       ├── screens/                # Screen composables
│       │   ├── main/               # Main screen
│       │   ├── processing/         # Processing screen with no-EXIF labels
│   │   ├── processingDetails/  # File list with thumbnails + context menu (View EXIF, Open with, Copy path, Share)
│       │   ├── metadataViewer/     # Bottom control bar (Pretty, Raw, Expand/Collapse)
│   │   ├── settings/           # Settings with use datetime as name toggle
│   │   ├── savePath/           # Save path selection (full-screen Scaffold with checkbox for skip)
│       │   └── help/               # Help screen
│       └── theme/                  # Color, Theme, Typography
├── build.gradle.kts                # Root build (plugin declarations)
├── app/build.gradle.kts            # App module build config
├── gradle/libs.versions.toml       # Version catalog
└── docs/                           # Project documentation
```

## Module Responsibilities

| Module | Depends on | What it does |
|---|---|---|
| `core/image` | nothing | Data models: `ImageFormat` (JPEG/PNG/WebP), `ImageMetadataSnapshot`, `ImageProcessingStep`, `ImageProcessingSummary`, `ImageProcessingProgress` |
| `core/di` | Hilt | DI modules: `ContextModule`, `CoroutineModule`, `DataStoreModule` |
| `core/extension` | nothing | Kotlin extensions for ClipData, Context, ContentResolver, Coroutine, List, Uri |
| `core/contract` | core/image | Activity result contracts: share images, take picture, view URL |
| `core/serializer` | Wire | Protobuf serializers for `ImageSources` and `Selection` |
| `main/business` | main/data, settings, core/image | `MainViewModel` - handles image source selection, clipboard paste, camera, shortcuts |
| `main/data` | core | `MainRepository` (clip, camera, file ops), `SelectionRepository` (protobuf), `ImageSourcesRepository` |
| `imageProcessing/data` | core/image, exif-interface | `ImageProcessingRepository` - scans images, removes metadata, saves results. Accepts `isUseDateTimeNameEnabled` parameters to control filename behavior |
| `imageProcessing/business` | imageProcessing/data, main/data, settings | `ImageProcessingViewModel` - orchestrates bulk EXIF removal with progress |
| `imageProcessingDetails/business` | imageProcessing/data, settings | `ImageProcessingDetailsViewModel` - handles processed file list, copy path |
| `metadataViewer/data` | metadata-extractor | `MetadataViewerRepository` - reads all metadata via `ImageMetadataReader` |
| `metadataViewer/business` | metadataViewer/data | `MetadataViewerViewModel` - loads metadata, toggles expand/collapse, raw/pretty mode |
| `settings/data` | DataStore | `SettingsRepository` - all preferences (paths, theme, auto-delete, etc.) |
| `settings/business` | settings/data | `SettingsViewModel` - manages settings UI state |
| `navigation` | all screens | `AppNavHost` - Navigation Compose graph, `Screen` sealed class |
| `ui/theme` | Material3 | `ExifEraserTheme` - Light/Dark/AMOLED themes with dynamic color |
| `ui/screens` | all ViewModels | Compose screens: Main, Processing, ProcessingDetails (file list with thumbnails + context menu), SavePath, MetadataViewer, Settings, Help |
| `ui/components` | nothing | Reusable composables: dialogs, cards, metadata items, loading/empty/error states |

## Data Flow

### EXIF Removal Flow

1. User selects images via MainScreen (pick single/multiple, directory, camera, or paste)
2. Images stored in protobuf via `SelectionRepository`
3. User navigates to save path selection (or skips if default configured)
4. `ImageProcessingViewModel` reads selection, calls `ImageProcessingRepository.removeMetadata(isUseDateTimeNameEnabled, isRandomizeFileNamesEnabled, displayNameSuffix)`
5. Repository scans each image with `ExifInterfaceExtended`, builds `ImageMetadataSnapshot`
6. Creates new document in save directory, writes metadata-stripped image
7. **Filename logic:** Priority order: Use Datetime → Randomize + Suffix → Default (original + camera suffix + suffix)
8. Optionally deletes original (auto-delete setting)
9. Emits `ImageProcessingStep.FinishedSingle` per image with summary
10. **No-EXIF handling:** When no metadata found, displays "Files already have no EXIF" (0 modified) or "Some files already had no EXIF" (partial cases)
11. Shows "View details" button for ProcessingDetailsScreen (for both single and multiple files)
12. User can view details, copy path, or share processed images

### Metadata Viewer Flow

1. User taps image on MainScreen to view metadata
2. `MetadataViewerScreen` receives URI and display name
3. `MetadataViewerViewModel.loadMetadata()` calls `MetadataViewerRepository.readMetadata()`
4. Repository uses `ImageMetadataReader.readMetadata()` (metadata-extractor library)
5. Returns list of `MetadataGroup` (directories) with `MetadataTag` entries
6. UI displays groups in expandable sections with bottom control bar (Pretty, Raw, Expand/Collapse buttons above navigation bar)

### Camera Flow

1. User takes photo via camera on MainScreen
2. Camera uses default save path from settings (falls back to save path picker if not set)
3. Photo saved with timestamp-based filename
4. User returns to app (back press goes back quietly, no false "processing complete" message)
5. Photo added to selection for processing

## Key Design Decisions

- **Orbit MVI** - All ViewModels use `ContainerHost` with `intent {}` / `reduce {}` / `postSideEffect{}`
- **Hilt DI** - `@HiltAndroidApp`, `@HiltViewModel`, `@Singleton` throughout
- **Wire protobuf** - Image sources and selections serialized with Wire for `SavedStateHandle`
- **DataStore** - All settings stored in Jetpack DataStore preferences
- **DocumentFile API** - File access via SAF (Storage Access Framework) for broad storage compatibility
- **Dual metadata approach** - exif-interface-extended for stripping, metadata-extractor for viewing
- **Edge-to-edge** - System bars handled in `MainActivity`, AMOLED gets black bars
- **Material3 themes** - Light, Dark, AMOLED (pure black), System (follows OS)
- **Dynamic colors** - Material You support on Android 12+, togglable in settings
- **Use datetime as name** - Toggle in settings uses processing timestamp as filename (format: yyyyMMdd_HHmmss_SSS)
- **Bottom control bar** - MetadataViewerScreen uses labeled buttons (Pretty, Raw, Expand/Collapse) above navigation bar
- **No-EXIF labels** - ProcessingScreen displays "Files already have no EXIF" (0 modified) or "Some files already had no EXIF" (partial)
- **Themed snackbar** - Error snackbars use #333 for AMOLED, surfaceVariant for light/dark themes
- **Context menu** - ProcessingDetailsScreen provides: View EXIF, Open with, Copy path, Share

## Common Tasks

**Add a new image format:** Edit `core/image/ImageFormat.kt` to add data class, update `supportedImageFormats` list, update MIME type filter in `core/util/Constants.kt`.

**Add a new setting:** Add key constant in `core/util/Constants.kt`, add DataStore read/write in `SettingsRepository`, add state field in `SettingsState`, add UI in `SettingsScreen`. Add icon to the toggle in SettingsScreen.

**Add a new screen:** Create `business/` package with ViewModel/State/SideEffect, create screen composable in `ui/screens/`, add `Screen` sealed class entry, register in `AppNavHost`.

**Add a new metadata tag display:** Edit `MetadataViewerRepository` to filter/transform, update `MetadataViewerScreen` UI components.

**Change theme behavior:** Edit `ui/theme/Theme.kt` (color schemes), `ThemeMode` enum in `settings/business/SettingsState.kt`, or `SettingsRepository` for persistence.

**Modify EXIF removal logic:** Edit `ImageProcessingRepository.removeMetaData()` for per-image processing, `ImageProcessingRepository.scanImage()` for detection.

**Add new UI component:** Create composable in `ui/components/`, follow existing patterns (e.g., `ConfirmDialog.kt`, `ImageSourceCard.kt`).

**After rebuilding the app:** Always run `adb install -r app/build/outputs/apk/debug/app-debug.apk` to deploy to connected devices. Check `adb devices` first — if no devices are connected, warn the user before proceeding with the build.

## Docs Index

- [Project Overview](docs/README.md) - Quick summary of what/why, architecture, and links to detailed docs
- [Architecture](docs/architecture/README.md) - Tech stack, module structure, data flow
- [Configuration](docs/configuration/README.md) - Build config, dependencies, theme system
- [Features](docs/features/README.md) - Feature list
  - [EXIF Removal](docs/features/exif-removal.md) - Processing flow, repository, metadata detection
  - [EXIF Viewer](docs/features/exif-viewer.md) - Metadata viewer, group/tag structure
  - [Themes](docs/features/themes.md) - ThemeMode, AMOLED, dynamic colors
- [Guides](docs/guides/README.md) - How-to guides
  - [Contributing](docs/guides/contributing.md) - Dev setup and conventions
