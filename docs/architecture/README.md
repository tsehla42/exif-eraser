# Architecture

## Tech Stack

| Component | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.4.0 |
| UI | Jetpack Compose | BOM 2026.09.00 |
| Material Design | Material3 | (via Compose BOM) |
| Navigation | Navigation Compose | 2.10.2 |
| DI | Dagger Hilt | 2.60.1 |
| MVI | Orbit | 12.0.1 |
| EXIF removal | exif-interface-extended | 1.1.0 |
| Metadata reading | metadata-extractor | 2.22.0 |
| Serialization | Wire (protobuf) | 7.1.2 |
| Preferences | DataStore | 1.3.0 |
| Image loading | Coil | 3.6.3 |
| Logging | Timber | 5.0.1 |
| Build | AGP | 9.2.1 |
| Min SDK | 28 | - |
| Target SDK | 37 | - |

## Module Structure

The app is a single-module project (`app/`). Code is organized by feature under `com.tsehla.exiferaser`:

```
core/                    # Shared code used across features
├── contract/            # AndroidX ActivityResultContracts
├── di/                  # Hilt DI modules (Context, Coroutine, DataStore)
├── extension/           # Kotlin extension functions (ContentResolver, ClipData, etc.)
├── image/               # Image data models (format, metadata, processing)
├── provider/            # CameraFileProvider for camera capture
├── serializer/          # Wire protobuf serializers
└── util/                # Constants and file utilities

main/                    # Main screen (image source selection)
├── business/            # MainViewModel, MainState, MainSideEffect
└── data/                # MainRepository, SelectionRepository, ImageSourcesRepository, ShareIntentHolder, ShareIntentParser, ShareIntentCoordinator

imageProcessing/         # EXIF removal processing
├── business/            # ImageProcessingViewModel, State, SideEffect
└── data/                # ImageProcessingRepository

imageProcessingDetails/  # Processing results detail view
└── business/            # DetailsViewModel, State, SideEffect

imageSavePathSelection/  # Save directory picker
└── business/            # SavePathViewModel, State, SideEffect

imageDetails/            # Single image metadata details
└── business/            # DetailsViewModel, State

metadataViewer/          # Full metadata viewer
├── business/            # MetadataViewerViewModel, State, SideEffect
└── data/                # MetadataViewerRepository

settings/                # App settings
├── business/            # SettingsViewModel, SettingsState, ThemeMode
└── data/                # SettingsRepository (DataStore-backed)

navigation/              # Navigation graph
├── AppNavHost.kt        # NavHost composable with all routes
└── Screen.kt            # Sealed class defining routes

ui/                      # Compose UI layer
├── components/          # Reusable composables (ConfirmDialog, GradientArrow, ThemeSettings, etc.)
├── screens/             # Screen composables
└── theme/               # Color, Theme, Typography
```

## Data Flow

### EXIF Removal

```
User selects images
  → MainViewModel puts selection in SelectionRepository (protobuf)
  → User navigates to SavePathScreen (or skips with default)
  → ImageProcessingViewModel reads selection
  → ImageProcessingRepository.removeMetadata(fileNameMode, displayNameSuffix) flow:
      For each image:
        1. Scan with ExifInterfaceExtended → ImageMetadataSnapshot
        2. If metadata found or fileNameMode != IMAGE_ID:
           a. Determine filename based on FileNameMode:
              - RANDOM: UUID v4 + suffix
              - IMAGE_ID: numeric media/picker ID (falls back to display name) + camera suffix + suffix
              - CONVERSION_DATE: timestamp yyyyMMdd_HHmmss_SSS
              - LAST_MODIFIED: EXIF date yyyyMMdd_HHmmss with fallback chain
           b. Create new document via DocumentsContract
           c. Write image without metadata via saveExclusive()
           d. Track converted + original URIs (no deletion during processing)
        3. Emit ImageProcessingStep.FinishedSingle with summary
      Finally: emit ImageProcessingStep.FinishedBulk
  → UI updates progress, shows results
  → Optional: auto-share if enabled
  → Completion screen: delete checkboxes (pre-checked from settings),
    Save elsewhere (move batch to picked folder), Share, OK (applies deletions + closes)
```

### Share to App

```
Another app shares 1+ images (ACTION_SEND / ACTION_SEND_MULTIPLE)
  → MainActivity parses the intent via ShareIntentParser
      (clipData via supportedImageUrisToList, EXTRA_STREAM fallback;
       intent marked with EXTRA_SHARE_CONSUMED so recreation doesn't reprocess)
  → URIs held in ShareIntentHolder until MainActivity observes
      ShareIntentCoordinator.pendingShare (activity-level, so it fires no
      matter which tab — Tools/Help/Settings — is open)
  → ShareIntentCoordinator.handlePendingShare():
      1. Consumes the URIs, writes selection to SelectionRepository
      2. Uses the default save path if valid, else opens the save path picker
      3. Empty/failed shares report Unsupported / SelectionFailed,
         shown as an activity-level snackbar
  → ImageProcessingViewModel reads selection and processes as usual
  → OK pops a single back-stack entry: returns to the tab that opened
      the conversion flow (or Tools after a cold start)
```

### Metadata Viewer

```
User taps image on MainScreen
  → Navigate to MetadataViewerScreen with URI + displayName
  → MetadataViewerViewModel.loadMetadata(uri)
    → MetadataViewerRepository.readMetadata(uri)
      → ImageMetadataReader.readMetadata(stream) [metadata-extractor]
      → Map directories to MetadataGroup with MetadataTag list
  → UI shows expandable groups with raw/pretty toggle
  → User taps delete button
    → MetadataViewerViewModel.removeMetadata(uri)
      → Stores image in SelectionRepository
      → Determines save path (from settings or save path picker)
      → Navigates to ImageProcessing screen
    → ImageProcessingViewModel processes the single image
```

### Settings

```
SettingsScreen reads from SettingsViewModel
  → SettingsRepository reads/writes DataStore<Preferences>
  → Theme mode, dynamic color, paths, and processing options persisted
  → Theme section uses ThemeSettings component (SectionHeader + Slider + Arrow + DynamicColor toggle)
  → ClearCache uses ConfirmDialog component
  → MainActivity observes theme settings via DataStore flow
  → ExifEraserTheme applies selected theme
```

## Key Dependencies

- **exif-interface-extended** (`io.github.tommy-geenexus:exifinterface-extended`) - Reads and strips EXIF data. Used for metadata detection (`hasAttributes`, `hasXmp`, `hasIccProfile`) and stripping (`saveExclusive`).
- **metadata-extractor** (`com.drewnoakes:metadata-extractor`) - Reads all image metadata (EXIF, IPTC, XMP, ICC, etc.) for display in MetadataViewer.
- **Orbit MVI** (`org.orbitmvi:orbit-viewmodel`) - Unidirectional data flow. ViewModels use `intent {}`, `reduce {}`, `postSideEffect{}`.
- **Wire** (`com.squareup.wire`) - Protobuf serialization for image selection and source lists in `SavedStateHandle`.
- **DataStore** (`androidx.datastore:datastore-preferences`) - Type-safe preferences storage for all settings.
