# EXIF Removal

## Overview

The EXIF removal feature strips metadata (EXIF, ICC, XMP, Photoshop resources) from JPEG, PNG, and WebP images and saves clean copies to a user-selected directory.

## Processing Flow

### 1. Image Selection

Users can select images via:
- **Single image** - Pick one image from gallery
- **Multiple images** - Pick multiple from gallery
- **Directory** - Process all images in a folder
- **Camera** - Take a photo, then process
- **Clipboard** - Paste images from clipboard

Selections are serialized to protobuf via `SelectionRepository` and stored in `SavedStateHandle`.

### 2. Save Path Selection

`SavePathScreen` lets users choose where processed images are saved. If "skip save path selection" is enabled and a default path exists, this step is bypassed.

### 3. Metadata Scanning

`ImageProcessingRepository.scanImage()` reads the source image and creates an `ImageMetadataSnapshot`:

```kotlin
data class ImageMetadataSnapshot(
    val isIccProfileContained: Boolean,
    val isExifContained: Boolean,
    val isPhotoshopImageResourcesContained: Boolean,
    val isXmpContained: Boolean,
    val isExtendedXmpContained: Boolean
)
```

Detection uses `ExifInterfaceExtended` methods:
- `hasIccProfile()` - ICC color profile
- `hasAttributes(true)` - EXIF attributes
- `hasPhotoshopImageResources()` - Photoshop metadata
- `hasXmp()` / `hasExtendedXmp()` - XMP data

### 4. Metadata Stripping

If metadata is found (or randomize filenames is enabled):

1. **Create output document** via `DocumentsContract.createDocument()` in the save directory
2. **Write clean image** via `ExifInterfaceExtended.saveExclusive()` which:
   - Copies image data without metadata
   - Optionally preserves orientation if `isPreserveOrientationEnabled`
3. **Delete original** if `isAutoDeleteEnabled` and the source is a document URI

### 5. Filename Formatting

Output filenames are determined by `FileNameMode` enum:
- `RANDOM` → UUID v4 filename + suffix (if provided)
- `IMAGE_ID` → original file picker ID + camera suffix (if from camera) + suffix
- `CONVERSION_DATE` → processing timestamp (format: yyyyMMdd_HHmmss_SSS)
- `LAST_MODIFIED` → EXIF DateTimeOriginal/DateTimeDigitized with fallback chain:
  1. EXIF `DateTimeOriginal` (when photo was taken)
  2. EXIF `DateTimeDigitized` (when photo was digitized)
  3. File system `lastModified` date
  4. Current date/time (same format as conversion but without milliseconds)

Note: EXIF DateTimeOriginal is second-resolution (no milliseconds), so LAST_MODIFIED uses `yyyyMMdd_HHmmss` format. CONVERSION_DATE uses `yyyyMMdd_HHmmss_SSS` with real milliseconds from `LocalDateTime.now()`.

## Key Classes

| Class | Location | Responsibility |
|---|---|---|
| `ImageProcessingRepository` | `imageProcessing/data/` | Scans images, strips metadata, saves results |
| `ImageProcessingViewModel` | `imageProcessing/business/` | Orchestrates bulk processing, tracks progress |
| `ImageProcessingState` | `imageProcessing/business/` | UI state: progress, summaries, counts |
| `ImageProcessingStep` | `core/image/` | Sealed class: Idle, FinishedSingle, FinishedBulk |
| `ImageProcessingSummary` | `core/image/` | Per-image result: display name, URI, metadata snapshot |
| `ImageMetadataSnapshot` | `core/image/` | What metadata was found in the image |

## Supported Formats

Defined in `core/image/ImageFormat.kt`:

| Format | MIME Type |
|---|---|
| JPEG | `image/jpeg` |
| PNG | `image/png` |
| WebP | `image/webp` |

## Processing Settings

| Setting | Default | Effect |
|---|---|---|
| Auto Delete | false | Delete source after processing |
| Preserve Orientation | false | Keep EXIF orientation in output |
| File Name Mode | IMAGE_ID | Controls output filename (RANDOM, IMAGE_ID, CONVERSION_DATE, LAST_MODIFIED) |
| Share by Default | false | Auto-share processed images |
| Display Name Suffix | "" | Append suffix to output filenames |
| Skip Save Path Selection | false | Use default save path directly |

## Progress Tracking

Processing emits `ImageProcessingStep.FinishedSingle` per image with:
- `ImageProcessingSummary` (result data)
- `ImageProcessingProgress` (percentage complete)

`ImageProcessingViewModel` tracks:
- `imagesProcessedCount` - Total processed
- `imagesWithMetadataCount` - Had metadata
- `imagesSavedCount` - Successfully saved
- `progress` - Current progress percentage

## Processing Results

After processing completes:

- **Single file**: "View new EXIF" button opens metadata viewer
- **Multiple files**: "View details" button opens file list with thumbnails
- Each file entry has a context menu with: View EXIF, Open with, Copy path, Share
- When images have no EXIF: "Files already have no EXIF" label
- When some images have no EXIF: "Some files already had no EXIF" label
- Shows saved count and modified count: "Saved X images, Y of Z modified"
- Copy path returns clean path format: `Pictures/exifEdit/filename.jpg`
