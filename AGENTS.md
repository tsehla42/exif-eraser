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
├── app/src/main/kotlin/com/tsehla/exiferaser/
│   ├── ExifEraserApplication.kt    # Hilt app entry, dynamic colors, Timber init
│   ├── MainActivity.kt             # Single activity, edge-to-edge, theme collection
│   ├── core/                       # Shared utilities
│   │   ├── contract/               # ActivityResultContracts (share, camera, view URL)
│   │   ├── di/                     # Hilt modules (Context, Coroutine, DataStore)
│   │   ├── extension/              # Extension functions (ClipData, Context, URI, ContentResolver, etc.)
│   │   ├── image/                  # Image models (format, metadata snapshot, processing, FileNameMode)
│   │   ├── provider/               # CameraFileProvider for camera capture
│   │   ├── serializer/             # Wire protobuf serializers
│   │   └── util/                   # Constants, file utils
│   ├── main/                       # Main screen feature
│   │   ├── business/               # MainViewModel, MainState, MainSideEffect
│   │   └── data/                   # MainRepository, SelectionRepository, ImageSourcesRepository, ShareIntentHolder, ShareIntentParser, ShareIntentCoordinator
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
│       ├── components/             # Reusable composables (dialogs, ShareSelectionDialog, cards, metadata items, GradientArrow, ThemeSettings)
│       ├── screens/                # Screen composables
│       │   ├── main/               # Main screen
│       │   ├── processing/         # Processing screen with no-EXIF labels
│   │   ├── processingDetails/  # File list with thumbnails + context menu (View EXIF, Open with, Copy path, Share)
│       │   ├── metadataViewer/     # Bottom control bar (Pretty, Raw, Expand/Collapse)
│   │   ├── settings/           # Settings with use datetime as name toggle
│   │   ├── savePath/           # Save path selection (full-screen Scaffold with checkbox for skip)
│       │   └── help/               # Help screen
│       └── theme/                  # Color, Theme, Typography
├── app/src/main/res/
│   ├── drawable/
│   │   ├── baseline_*.xml               # Material Symbols UI icons (35 icons, Google Fonts + simple-icons Git logo)
│   │   ├── ic_launcher_foreground.xml   # Adaptive icon foreground (vector)
│   │   └── ic_launcher_monochrome.xml   # Themed icon monochrome layer (vector)
│   ├── mipmap-anydpi-v26/
│   │   ├── ic_launcher.xml              # Adaptive icon definition (bg+fg+mono)
│   │   └── ic_launcher_round.xml        # Round adaptive icon definition
│   ├── values/colors.xml                # Icon colors (light) + app colors
│   ├── values-v31/colors.xml            # Icon colors (Android 12+ dynamic)
│   ├── values-night/colors.xml          # Icon colors (dark mode)
│   └── values-night-v31/colors.xml      # Icon colors (dark + dynamic)
├── build.gradle.kts                # Root build (plugin declarations)
├── app/build.gradle.kts            # App module build config
├── gradle/libs.versions.toml       # Version catalog
└── docs/                           # Project documentation
```

## Module Responsibilities

| Module | Depends on | What it does |
|---|---|---|
| `core/image` | nothing | Data models: `ImageFormat` (JPEG/PNG/WebP), `ImageMetadataSnapshot`, `ImageProcessingStep`, `ImageProcessingSummary`, `ImageProcessingProgress`, `FileNameMode` |
| `core/di` | Hilt | DI modules: `ContextModule`, `CoroutineModule`, `DataStoreModule` |
| `core/extension` | nothing | Kotlin extensions for ClipData, Context, ContentResolver, Coroutine, List, Uri |
| `core/contract` | core/image | Activity result contracts: share images, take picture, view URL |
| `core/serializer` | Wire | Protobuf serializers for `ImageSources` and `Selection` |
| `main/business` | main/data, settings, core/image | `MainViewModel` - handles image source selection, clipboard paste, camera, shortcuts |
| `main/data` | core | `MainRepository` (clip, camera, file ops), `SelectionRepository` (protobuf), `ImageSourcesRepository`, `ShareIntentHolder` (pending shared URIs), `ShareIntentParser` (ACTION_SEND extraction), `ShareIntentCoordinator` (processes pending shares regardless of the visible tab) |
| `imageProcessing/data` | core/image, exif-interface | `ImageProcessingRepository` - scans images, removes metadata, saves results. Accepts `FileNameMode` to control filename behavior |
| `imageProcessing/business` | imageProcessing/data, main/data, settings | `ImageProcessingViewModel` - orchestrates bulk EXIF removal with progress |
| `imageProcessingDetails/business` | imageProcessing/data, settings | `ImageProcessingDetailsViewModel` - handles processed file list, copy path |
| `metadataViewer/data` | metadata-extractor | `MetadataViewerRepository` - reads all metadata via `ImageMetadataReader` |
| `metadataViewer/business` | metadataViewer/data, main/data, settings | `MetadataViewerViewModel` - loads metadata, toggles expand/collapse, raw/pretty mode, handles EXIF removal via delete button |
| `settings/data` | DataStore | `SettingsRepository` - all preferences (paths, theme, auto-delete, etc.) |
| `settings/business` | settings/data | `SettingsViewModel` - manages settings UI state |
| `navigation` | all screens | `AppNavHost` - Navigation Compose graph, `Screen` sealed class |
| `ui/theme` | Material3 | `ExifEraserTheme` - Light/Dark/AMOLED themes with dynamic color |
| `ui/screens` | all ViewModels | Compose screens: Main, Processing, ProcessingDetails (file list with thumbnails + context menu), SavePath, MetadataViewer, Settings, Help |
| `ui/components` | nothing | Reusable composables: `ConfirmDialog`, `ShareSelectionDialog` (three-outcome share dialog: All files / Only converted / tap-outside cancels), `ImageSourceCard`, `GradientArrow` (gradient-filled chevron arrow), `ThemeSettings` (bundled theme settings with SectionHeader, Slider, Arrow, DynamicColor toggle), metadata items, loading/empty/error states, `DiscreteSliderPreference` |

## Data Flow

### EXIF Removal Flow

1. User selects images via MainScreen (pick single/multiple, directory, camera, paste, or share from another app)
2. Images stored in protobuf via `SelectionRepository`
3. Save path: the default save path from settings is used automatically when set — the save-path picker is only shown when no valid default exists
4. `ImageProcessingViewModel` reads selection, calls `ImageProcessingRepository.removeMetadata(fileNameMode, displayNameSuffix)`
5. Repository scans each image with `ExifInterfaceExtended`, builds `ImageMetadataSnapshot`
6. Creates new document in save directory, writes metadata-stripped image
7. **Filename logic:** `FileNameMode` enum controls output:
   - `RANDOM` → UUID v4 filename + suffix
   - `IMAGE_ID` → numeric media/picker ID of the source image via `extractImageId()` (falls back to the source display name when the URI carries no ID) + camera suffix + suffix
   - `CONVERSION_DATE` → processing timestamp (yyyyMMdd_HHmmss_SSS)
   - `LAST_MODIFIED` → EXIF date with fallback chain (DateTimeOriginal → DateTimeDigitized → file date → current date), format: yyyyMMdd_HHmmss (no milliseconds since EXIF is second-resolution)
8. Tracks converted and original URIs in state — nothing is deleted during processing
9. Emits `ImageProcessingStep.FinishedSingle` per image with summary
10. **No-EXIF handling:** When no metadata found, displays "Files already have no EXIF" (0 modified) or "Some files already had no EXIF" (partial cases)
11. Shows "View details" button for ProcessingDetailsScreen (for both single and multiple files)
12. Completion screen offers: a single "Delete files: Originals / Converted" checkbox row (pre-checked from the two settings, per-run only — toggling never writes back to settings), a "Save elsewhere" button below Share/OK (moves this batch's files to a user-picked folder without changing the default save path), Share (never deletes; for a mixed batch — some files with EXIF, some without — it first opens a dialog offering "All files" or "Only converted", where "converted" means files that actually had EXIF cleared — a no-EXIF file counts as unconverted even when non-IMAGE_ID naming copied it — and a batch where no file had EXIF shares everything directly, with the Share button showing whenever files were processed), and OK (deletes converted files silently; originals that are MediaStore items go through Android's system consent dialog `MediaStore.createTrashRequest` — "Allow <app> to move N photos to bin?" — so they land in the system trash and stay restorable for ~30 days; app-owned camera cache files are deleted directly; SAF folder originals are deleted via their existing folder grant; originals owned by another app's FileProvider can't be deleted and show a "Couldn't delete original files" snackbar instead of closing — the screen stays open until the user acts)

### Metadata Viewer Flow

1. User taps image on MainScreen to view metadata
2. `MetadataViewerScreen` receives URI and display name
3. `MetadataViewerViewModel.loadMetadata()` calls `MetadataViewerRepository.readMetadata()`
4. Repository uses `ImageMetadataReader.readMetadata()` (metadata-extractor library)
5. Returns list of `MetadataGroup` (directories) with `MetadataTag` entries
6. UI displays groups in expandable sections with bottom control bar (Pretty, Raw, Expand/Collapse buttons above navigation bar)
7. User can tap delete button to remove EXIF from the viewed image
8. `MetadataViewerViewModel.removeMetadata()` stores image in `SelectionRepository`, determines save path
9. Navigates to `ImageProcessing` screen which processes the single image

### Camera Flow

1. User takes photo via camera on MainScreen
2. Camera uses default save path from settings (falls back to save path picker if not set)
3. Photo saved with timestamp-based filename
4. User returns to app (back press goes back quietly, no false "processing complete" message)
5. Photo added to selection for processing

### Share Flow

1. User selects 1+ photos in another app (e.g. Photos) and shares to Exif Eraser (`ACTION_SEND`/`ACTION_SEND_MULTIPLE`, manifest filters for jpeg/png/webp)
2. `MainActivity` extracts URIs via `ShareIntentParser` (clipData first, `EXTRA_STREAM` fallback), marks the intent consumed (prevents reprocessing on recreation), and submits to `ShareIntentHolder`
3. `MainActivity` collects pending URIs from `ShareIntentCoordinator` (activity-level, so a share is picked up immediately no matter which tab — Tools/Help/Settings — is open); the coordinator consumes the URIs and writes the selection to `SelectionRepository`
4. The default save path is used if valid — the save-path picker only appears when no valid default exists; unsupported/empty or failed shares show an "Unsupported image content" / "Failed to save image(s)" snackbar from the activity-level `ThemedSnackbarHost`
5. Navigates straight to `ImageProcessing` which auto-runs; tapping OK pops a single back-stack entry, so it returns to the screen that opened the conversion flow (the previously opened tab, or Tools after a cold start)

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
- **Discrete slider settings** - File naming uses `DiscreteSliderPreference` with Canvas gradient arrows (primary → onSurfaceVariant); theme uses `ThemeSettings` component
- **FileNameMode enum** - Replaces boolean toggles for file naming (RANDOM, IMAGE_ID, CONVERSION_DATE, LAST_MODIFIED)
- **Gradient arrows** - Canvas-drawn with `Brush.linearGradient()`, labels at left/right edges
- **Always-visible descriptions** - File name slider shows description text without collapse/expand
- **Bottom control bar** - MetadataViewerScreen uses labeled buttons (Pretty, Raw, Expand/Collapse) above navigation bar
- **Persistent bottom nav** - Bottom navigation bar (Help, Tools, Settings) visible on MainScreen, SettingsScreen, HelpScreen, and MetadataViewerScreen with active tab highlighted
- **No-EXIF labels** - ProcessingScreen displays "Files already have no EXIF" (0 modified) or "Some files already had no EXIF" (partial)
- **Themed snackbar** - Error snackbars use #333 for AMOLED, surfaceVariant for light/dark themes
- **Context menu** - ProcessingDetailsScreen provides: View EXIF, Open with, Copy path, Share
- **Share scope dialog** - The share choice keys on **EXIF presence** (`imageMetadataSnapshot.isMetadataContained()`), not on whether a copy was written: non-IMAGE_ID naming modes copy no-EXIF files too, so keying on `isImageSaved` would hide the dialog for exactly those batches (regression caught on-device 2026-11-09; "converted" = had EXIF **and** a saved cleaned copy)
- **Activity-level share handoff** - Pending shares are consumed by `MainActivity` via `ShareIntentCoordinator`, never inside a screen's `LaunchedEffect`: a screen (e.g. `MainScreen`) is disposed whenever another tab or route is on top, which previously left shares stuck until the user tapped Tools
- **Material Symbols icons** - UI icons use XML drawables from Google Fonts (`fonts.google.com/icons`) loaded via `painterResource()`, not the deprecated `material-icons-extended` library (exception: the Help screen Repository button uses the Git logo from simple-icons, tinted monochrome like the rest)

## Common Tasks

**Add a new setting:** Add key constant in `core/util/Constants.kt`, add DataStore read/write in `SettingsRepository`, add state field in `SettingsState`, add UI in `SettingsScreen`. For enum-based settings (like `FileNameMode`), add the enum to `core/image/`, store as int ordinal in DataStore with migration from old keys.

**Add a new screen:** Create `business/` package with ViewModel/State/SideEffect, create screen composable in `ui/screens/`, add `Screen` sealed class entry, register in `AppNavHost`.

**Add a new metadata tag display:** Edit `MetadataViewerRepository` to filter/transform, update `MetadataViewerScreen` UI components.

**Change theme behavior:** Edit `ui/theme/Theme.kt` (color schemes), `ThemeMode` enum in `settings/business/SettingsState.kt`, or `SettingsRepository` for persistence.

**Modify EXIF removal logic:** Edit `ImageProcessingRepository.removeMetaData()` for per-image processing, `ImageProcessingRepository.scanImage()` for detection.

**Add new UI component:** Create composable in `ui/components/`, follow existing patterns (e.g., `ConfirmDialog.kt`, `ImageSourceCard.kt`).

**Add a new icon:** Download from `fonts.google.com/icons` (Material Symbols, Outlined style). Get the Android XML via the Android tab, or fetch SVG path data from `api.iconify.design/material-symbols.json?icons=<name>` and convert to Android vector drawable. Place in `app/src/main/res/drawable/baseline_<name>_24.xml` with `android:tint="?attr/colorControlNormal"`. Use via `painterResource(R.drawable.baseline_<name>_24)` in Compose.

**After rebuilding the app:** Always run `adb install -r app/build/outputs/apk/debug/app-debug.apk` to deploy to connected devices. Check `adb devices` first — if no devices are connected, warn the user before proceeding with the build.

**Note:** Entries like `adb-A1B2C3D4E5F6-AbCdEfGh._adb-tls-connect._tcp` are wireless-debugging TLS endpoints, and one physical device can appear twice — once as an `IP:port` entry (e.g. `192.168.1.100:43023`) and once as its TLS endpoint (e.g. `adb-SERIAL0001-AbCdEfGh._adb-tls-connect._tcp`). Despite older guidance, installs work directly on TLS endpoint entries too (verified 2026-11-09: `adb -s "adb-A1B2C3D4E5F6-AbCdEfGh._adb-tls-connect._tcp" install -r app/build/outputs/apk/debug/app-debug.apk` → Success on a device that had no `IP:port` entry). When available, run `awc --list` — it prints device names, IPs, and serials (e.g. "PhoneA (ModelX) · 192.168.1.100 SERIAL0001", "PhoneB (ModelY) · 192.168.1.101 A1B2C3D4E5F6") so you can tell which `adb devices` entry is which and install to every real device.

## Screenshots

If a request is ambiguous, complex, or hard to understand from text alone, take a screenshot from the connected device to see the current state:
```bash
adb -s <device> exec-out screencap -p > /tmp/screenshot.png
```
If no device is connected and a screenshot would help clarify the request, ask the user to provide one.

**Change the app icon:** Replace the SVG paths in `app/src/main/res/drawable/ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml`. Colors are in `res/values/colors.xml` (light), `values-night/colors.xml` (dark), `values-v31/colors.xml` (Android 12+ dynamic), `values-night-v31/colors.xml` (dark + dynamic). The adaptive icon uses `@color/ic_launcher_background` and `@color/ic_launcher_foreground`.

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
