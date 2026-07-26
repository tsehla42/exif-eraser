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
- **Share** - Receive images shared from another app (zero taps: goes straight to processing using the default save path; the save-path picker appears only when no valid default exists)

Selections are serialized to protobuf via `SelectionRepository` and stored in `SavedStateHandle`.

### 2. Save Path Selection

The default save path from settings is used automatically whenever it is set. `SavePathScreen` is only shown when no valid default exists; it is a pure picker (no skip checkbox).

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
3. **No deletion happens during processing** — converted and original URIs are tracked in state so the completion screen can delete them after the user taps OK

### 5. Filename Formatting

Output filenames are determined by `FileNameMode` enum:
- `RANDOM` → UUID v4 filename + suffix (if provided)
- `IMAGE_ID` → numeric media/picker ID of the source image (falls back to the source display name when the URI carries no ID) + camera suffix (if from camera) + suffix
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
| `ImageProcessingSummary` | `core/image/` | Per-image result: display name, URI, source URI, metadata snapshot |
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
| Auto Delete Originals | false | Pre-checks "Delete original files" on the completion screen |
| Delete Converted Files | false | Pre-checks "Delete converted files" on the completion screen |
| Preserve Orientation | false | Keep EXIF orientation in output |
| File Name Mode | IMAGE_ID | Controls output filename (RANDOM, IMAGE_ID, CONVERSION_DATE, LAST_MODIFIED) |
| Share by Default | false | Auto-share processed images |
| Display Name Suffix | "" | Append suffix to output filenames |

Completion-screen checkbox toggles are per-run only and never write back to these settings.

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

## Completion Screen Actions

When processing completes, the completion screen shows:

- A single **Delete files:** row — `Originals [ ] Converted [ ]` checkboxes, pre-checked from settings, per-run only
- **Share** and **OK** buttons — **Share** shows whenever files were processed; for a mixed batch (some files with EXIF, some without) it first opens a dialog offering **All files** or **Only converted**, where "converted" means files that actually had EXIF cleared (a no-EXIF file is unconverted even if naming mode copied it), and a batch where no file had EXIF shares everything directly without the dialog
- **Save elsewhere** below them — opens a folder picker and moves this batch's saved files there (direct document move when the provider supports it, otherwise copy-then-delete); the default save path setting is unchanged
- **Share** never deletes anything. **OK** applies the checked delete options, then closes:
  - *Converted files* are deleted silently (they live in the app's save folder)
  - *Originals* that are MediaStore items go through Android's system consent dialog (`MediaStore.createTrashRequest` — "Allow \<app\> to move N photos to bin?"), landing in the system trash where they stay restorable for ~30 days; the screen closes after the dialog resolves. App-owned camera cache files are deleted directly, and SAF folder originals via their existing folder grant. Originals owned by another app's FileProvider can't be deleted — a "Couldn't delete original files" snackbar is shown instead of closing.
