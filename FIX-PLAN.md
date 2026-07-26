# Fix Plan: 3 Bugs + Metadata Viewer → EXIF Removal Flow

## Fix 1: Make "View details" clickable

**File:** `app/src/main/kotlin/com/none/tom/exiferaser/ui/screens/processing/ProcessingScreen.kt`

**Problem:** Line 179-185, the "View details" is a plain `Text` with no click handler. `onDetailsClick` is passed but unused.

**Fix:** Wrap in `TextButton`:

```kotlin
if (hasSummaries) {
    Spacer(modifier = Modifier.height(24.dp))
    TextButton(onClick = onDetailsClick) {
        Text(
            text = stringResource(R.string.view_details),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
```

## Fix 2: Pass processing summaries to details screen

**Problem:** `ImageProcessingDetailsViewModel` reads from `savedStateHandle.get("navArgImageProcessingSummaries")` but Compose Navigation doesn't pass arguments on the `"image_processing_details"` route.

**Approach:** Use a shared `MutableState<List<ImageProcessingSummary>>` in a Hilt-scoped singleton, or simpler: serialize the summaries into a navigation argument as a JSON string.

**Simpler approach:** Use a `SharedFlow` in a shared container. But the cleanest approach for now: pass summaries as a JSON-encoded string argument via Compose Navigation.

**Files to change:**

### `app/src/main/kotlin/com/none/tom/exiferaser/navigation/Screen.kt`
Update `ImageProcessingDetails` to support an argument:
```kotlin
data object ImageProcessingDetails : Screen("image_processing_details/{summariesJson}") {
    fun createRoute(summariesJson: String) = "image_processing_details/${summarsJson}"
}
```

### `app/src/main/kotlin/com/none/tom/exiferaser/navigation/AppNavHost.kt`
- Add `navArgument("summariesJson") { type = NavType.StringType }` to the route
- Pass summaries from `ProcessingScreen` → navController
- Decode JSON in `ProcessingDetailsScreen`

### `app/src/main/kotlin/com/none/tom/exiferaser/ui/screens/processing/ProcessingScreen.kt`
- After processing completes, encode summaries as JSON and pass to `onNavigateToDetails(summariesJson)`
- Change `onNavigateToDetails` signature to accept `String` parameter

### `app/src/main/kotlin/com/none/tom/exiferaser/ui/screens/processingDetails/ProcessingDetailsScreen.kt`
- Accept `summariesJson: String` parameter
- Decode JSON to `List<ImageProcessingSummary>`
- Pass to ViewModel

### `app/src/main/kotlin/com/none/tom/exiferaser/imageProcessingDetails/business/ImageProcessingDetailsViewModel.kt`
- Remove `savedStateHandle.get("navArgImageProcessingSummaries")` fallback
- Accept summaries directly via `handleImageProcessingSummaries(summaries)` called from the screen

## Fix 3: Add "Inspect EXIF" card to MainScreen

**File:** `app/src/main/kotlin/com/none/tom/exiferaser/ui/screens/main/MainScreen.kt`

Add a 5th card to the grid for "Inspect EXIF metadata". This opens a photo picker, then navigates to `MetadataViewerScreen` with the selected image's URI.

**Changes:**
- Add `ImageSource` entry for metadata viewer with `Icons.Outlined.Info` or `Icons.Outlined.Search`
- Use a `PickVisualMedia` launcher (already available) to select an image
- On selection, navigate to `MetadataViewerScreen` with the URI
- Add string resource: `inspect_exif`

### `app/src/main/res/values/strings.xml`
Add: `<string name="inspect_exif">Inspect EXIF</string>`

## Fix 4: Add "Remove metadata" button on MetadataViewerScreen

**File:** `app/src/main/kotlin/com/none/tom/exiferaser/ui/screens/metadataViewer/MetadataViewerScreen.kt`

Add a FAB or bottom bar button: "Remove metadata from this image"
- When tapped, navigates to the image processing flow with this specific image
- OR shows a confirmation dialog then strips metadata

**Approach:** Add an `onRemoveMetadata: (Uri) -> Unit` callback to the screen, wire it in AppNavHost to navigate to `ImageProcessing` with the image URI.

**Changes:**

### `app/src/main/kotlin/com/none/tom/exiferaser/ui/screens/metadataViewer/MetadataViewerScreen.kt`
- Add a `FloatingActionButton` or bottom action: "Remove metadata"
- Callback: `onRemoveMetadata(uri)`

### `app/src/main/kotlin/com/none/tom/exiferaser/navigation/AppNavHost.kt`
- Pass `onRemoveMetadata` to `MetadataViewerScreen`
- When triggered: put the image into selection, navigate to processing

### `app/src/main/res/values/strings.xml`
Add: `<string name="remove_metadata">Remove metadata</string>`

## Implementation Order

1. Fix 1 (trivial — 1 file)
2. Fix 2 (medium — 4 files)
3. Fix 3 (medium — 2 files)
4. Fix 4 (medium — 2 files)

## Summary of files to change

| File | Fix |
|------|-----|
| `ProcessingScreen.kt` | #1 (clickable details) + #2 (pass summaries) |
| `ProcessingDetailsScreen.kt` | #2 (receive summaries) |
| `ImageProcessingDetailsViewModel.kt` | #2 (accept summaries directly) |
| `Screen.kt` | #2 (add route arg) |
| `AppNavHost.kt` | #2, #3, #4 (wiring) |
| `MainScreen.kt` | #3 (add inspect card) |
| `MetadataViewerScreen.kt` | #4 (add remove button) |
| `strings.xml` | #3, #4 (new strings) |
