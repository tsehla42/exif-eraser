# Features

## Feature List

| Feature | Description | Key Files |
|---|---|---|
| [EXIF Removal](exif-removal.md) | Strip metadata from JPEG/PNG/WebP images | `imageProcessing/data/ImageProcessingRepository.kt` |
| [Bulk Processing](exif-removal.md) | Process multiple images or directories | `imageProcessing/business/ImageProcessingViewModel.kt` |
| [EXIF Viewer](exif-viewer.md) | Display all metadata from any image | `metadataViewer/data/MetadataViewerRepository.kt` |
| [Themes](themes.md) | Light/Dark/AMOLED/System with dynamic colors | `ui/theme/Theme.kt` |
| Camera Capture | Take photo and process directly | `core/contract/ActivityResultContractTakePicture.kt` |
| Clipboard Paste | Paste images from clipboard (button on main screen) | `main/business/MainViewModel.kt` |
| Share to App | Receive shared images from other apps and strip EXIF immediately (zero taps; default save path) | `main/data/ShareIntentParser.kt`, `main/data/ShareIntentCoordinator.kt` |
| Image Directory | Process entire directories | `main/data/MainRepository.kt` |
| Auto Delete Originals | Pre-checks "Delete original files" on the completion screen | `settings/data/SettingsRepository.kt` |
| Delete Converted Files | Pre-checks "Delete converted files" on the completion screen | `settings/data/SettingsRepository.kt` |
| Save Elsewhere | Move the processed batch to a different folder from the completion screen | `imageProcessing/data/ImageProcessingRepository.kt` |
| Randomize Filenames | UUID-based output filenames | `imageProcessing/data/ImageProcessingRepository.kt` |
| Preserve Orientation | Keep EXIF orientation on output | `imageProcessing/data/ImageProcessingRepository.kt` |
| Share by Default | Auto-share after processing | `settings/data/SettingsRepository.kt` |
| Custom Suffix | Append suffix to output filenames | `settings/data/SettingsRepository.kt` |
| Legacy Image Selection | Fallback image picker on older devices | `settings/data/SettingsRepository.kt` |
| Use Datetime as Name | Use processing timestamp as filename (CONVERSION_DATE: yyyyMMdd_HHmmss_SSS, LAST_MODIFIED: yyyyMMdd_HHmmss) | `settings/data/SettingsRepository.kt` |
| Clear Cache | Clear cached images with size display | `ui/screens/settings/SettingsScreen.kt` |
| Processing Details | View list of processed files with thumbnails | `ui/screens/processingDetails/ProcessingDetailsScreen.kt` |
| Themed Snackbar | Error messages themed for AMOLED/Dark/Light | `ui/screens/main/MainScreen.kt` |
| Adaptive Icon | Material You dynamic color launcher icon | `res/values/colors.xml`, `res/drawable/ic_launcher_foreground.xml` |
