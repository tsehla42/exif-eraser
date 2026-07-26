# ExifEraser

This is a project based on [Tommy-Geenexus/exif-eraser](https://github.com/Tommy-Geenexus/exif-eraser).


## Description
ExifEraser is a modern, permissionless image metadata erasing application for Android.

It relies on Tommy-Geenexus [exif-interface-extended](https://github.com/Tommy-Geenexus/exif-interface-extended) under the hood.<p>

## Supported Formats

| Format | Metadata Removed |
|---|---|
| **JPEG** | ICC Profile, EXIF, Photoshop Resources, XMP/ExtendedXMP |
| **PNG** | ICC Profile, EXIF, XMP |
| **WebP** | ICC Profile, EXIF, XMP |

### Why does it exist?

The original exif-eraser was [deprecated](https://github.com/Tommy-Geenexus/exif-eraser#notice) with no additional features planned.  This project is a rewrite from scratch using LLM.

This project is a new frontend for [exif-interface-extended](https://github.com/Tommy-Geenexus/exif-interface-extended), which remains maintained by its original author.

The codebase is 90%+ different than the original. Same core EXIF logic via `exif-interface-extended`, but entirely new UI layer built with Jetpack Compose instead of XML Views.

## Features:

### Metadata Viewer
- **Full Metadata Display** — View all metadata from any image using `metadata-extractor` library
- **Expandable Groups** — Metadata organized by directory (EXIF, IPTC, XMP, ICC, etc.)
- **Raw/Pretty Toggle** — Switch between human-readable and raw tag values
- **One-Tap EXIF Removal** — Delete metadata directly from the viewer
- Processing details with thumbnails and context menu (View EXIF, Open with, Copy path, Share)

### Themes
- **Light / Dark / AMOLED** — Pure black backgrounds for OLED screens
- **System** — Follows device theme setting
- **Dynamic Colors** — Material You support on Android 12+, togglable in settings
- Adaptive icon with dynamic color support
- Themed snackbar messages

### File Naming
- **Random UUID** — UUID v4 filenames
- **Image ID** — Original file picker ID with suffix
- **Conversion Date** — Processing timestamp (yyyyMMdd_HHmmss_SSS)
- **Last Modified** — EXIF date with fallback chain

## Building

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Localization 

Original project had the translations on Crowdin, but this one does not. I plan adding the translations for this project in future.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

