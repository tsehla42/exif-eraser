# EXIF Viewer

## Overview

The metadata viewer displays all metadata embedded in an image using the `metadata-extractor` library. Unlike EXIF removal which uses `exif-interface-extended`, the viewer uses Drew Noakes' library to read the full range of metadata formats.

## How It Works

### 1. Loading

`MetadataViewerViewModel.loadMetadata(uri, displayName)`:
1. Sets loading state
2. Calls `MetadataViewerRepository.readMetadata(uri)`
3. Repository opens input stream via `ContentResolver`
4. `ImageMetadataReader.readMetadata(stream)` parses all metadata
5. Directories with tags are mapped to `MetadataGroup` objects
6. Groups and tags are sorted alphabetically

### 2. Data Model

```kotlin
data class MetadataGroup(
    val name: String,          // Directory name (e.g., "EXIF", "GPS", "XMP")
    val tags: List<MetadataTag>,
)

data class MetadataTag(
    val tagName: String,       // Tag name (e.g., "Camera Make")
    val value: String,         // Raw value
    val description: String,   // Human-readable description
)
```

### 3. Display

`MetadataViewerScreen` shows:
- Image name at top
- Expandable/collapsible groups (via `expandAll()` / `collapseAll()`)
- Each group shows its tags in a sorted list
- Toggle between raw and pretty display modes

### 4. Display Modes

| Mode | Description |
|---|---|
| Raw | Shows `tagValue` (raw metadata value) |
| Pretty | Shows `tagDescription` (human-readable description) |

Toggle via `MetadataViewerViewModel.toggleDisplayMode()`.

### Bottom Control Bar

The metadata viewer has a bottom control bar above the navigation bar with labeled buttons:

- **Pretty** - Switch to grouped view (expandable sections)
- **Raw** - Switch to raw monospace view
- **Expand/Collapse** - Single toggle button that expands or collapses all groups

The toggle buttons show the current mode with primary color highlighting. Expand/Collapse only appears in pretty mode.

## Key Classes

| Class | Location | Responsibility |
|---|---|---|
| `MetadataViewerRepository` | `metadataViewer/data/` | Reads metadata via metadata-extractor |
| `MetadataViewerViewModel` | `metadataViewer/business/` | Loads metadata, manages expand/collapse state, handles EXIF removal |
| `MetadataViewerState` | `metadataViewer/business/` | UI state: groups, expanded set, raw mode, loading |
| `MetadataViewerSideEffect` | `metadataViewer/business/` | Navigation side effects: `NavigateToProcessing`, `NavigateToSavePathSelection` |
| `MetadataGroup` | `metadataViewer/data/` | Directory with name and tags |
| `MetadataTag` | `metadataViewer/data/` | Single metadata entry |

## Metadata Formats Detected

The `metadata-extractor` library can read:
- **EXIF** - Camera settings, GPS, timestamps, thumbnails
- **IPTC** - News/photo metadata (caption, keywords, credits)
- **XMP** - Extensible Metadata Platform (Adobe standard)
- **ICC** - Color profiles
- **Photoshop** - Image resources
- **JFIF** - JPEG File Interchange Format
- **PNG** - PNG text chunks, gamma, chromaticity
- **WebP** - WebP-specific metadata
- **RIFF** - Resource Interchange File Format
- **Camera-specific** - Canon, Nikon, Sony, Fuji, etc.

## Navigation

From MainScreen, user taps an image → navigates to `MetadataViewerScreen` with URI and display name. The screen has a "Remove Metadata" delete button (FAB) that:

1. Stores the image URI in `SelectionRepository`
2. Determines save path (from settings or save path picker)
3. Navigates to `ImageProcessing` screen to process the single image

The delete button is hidden when navigating from `ProcessingDetailsScreen` (via `hideDelete = true`).
