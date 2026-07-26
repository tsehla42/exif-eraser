# ExifEraser Documentation

A Jetpack Compose Android app that removes EXIF metadata from images and displays full metadata information.

## Quick Links

- [Architecture](architecture/README.md) - Tech stack, modules, data flow
- [Configuration](configuration/README.md) - Build config, dependencies, themes
- [Features](features/README.md) - Feature list and details
  - [EXIF Removal](features/exif-removal.md) - How metadata stripping works
  - [EXIF Viewer](features/exif-viewer.md) - How metadata display works
  - [Themes](features/themes.md) - Light/Dark/AMOLED theme system
- [Guides](guides/README.md) - Development how-to guides
  - [Contributing](guides/contributing.md) - Setup and conventions

## What This App Does

ExifEraser is an Android app that:

1. **Removes EXIF metadata** from JPEG, PNG, and WebP images using `exif-interface-extended`
2. **Displays full metadata** from any image using `metadata-extractor` library
3. **Supports bulk processing** of multiple images or entire directories
4. **Offers flexible save options** including auto-delete originals, custom suffixes, and randomized filenames
5. **Provides AMOLED-friendly themes** with pure black backgrounds and Material You dynamic colors
