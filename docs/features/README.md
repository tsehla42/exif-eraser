# Features

## Feature List

| Feature | Description | Key Files |
|---|---|---|
| [EXIF Removal](exif-removal.md) | Strip metadata from JPEG/PNG/WebP images | `imageProcessing/data/ImageProcessingRepository.kt` |
| [Bulk Processing](exif-removal.md) | Process multiple images or directories | `imageProcessing/business/ImageProcessingViewModel.kt` |
| [EXIF Viewer](exif-viewer.md) | Display all metadata from any image | `metadataViewer/data/MetadataViewerRepository.kt` |
| [Themes](themes.md) | Light/Dark/AMOLED/System with dynamic colors | `ui/theme/Theme.kt` |
| Camera Capture | Take photo and process directly | `core/contract/ActivityResultContractTakePicture.kt` |
| Clipboard Paste | Paste images from clipboard | `main/business/MainViewModel.kt` |
| Image Directory | Process entire directories | `main/data/MainRepository.kt` |
| Auto Delete | Optionally delete originals after processing | `settings/data/SettingsRepository.kt` |
| Randomize Filenames | UUID-based output filenames | `imageProcessing/data/ImageProcessingRepository.kt` |
| Preserve Orientation | Keep EXIF orientation on output | `imageProcessing/data/ImageProcessingRepository.kt` |
| Share by Default | Auto-share after processing | `imageProcessing/business/ImageProcessingViewModel.kt` |
| Custom Suffix | Append suffix to output filenames | `settings/data/SettingsRepository.kt` |
| Save Path Skip | Skip path selection with default | `settings/data/SettingsRepository.kt` |
| Legacy Image Selection | Fallback image picker on older devices | `settings/data/SettingsRepository.kt` |
