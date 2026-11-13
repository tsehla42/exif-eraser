/*
 * Copyright (c) 2018-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tsehla.exiferaser.imageProcessing.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.provider.MediaStore

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.tsehla.exiferaser.UserImageSelectionProto
import com.tsehla.exiferaser.core.di.DispatcherIo
import com.tsehla.exiferaser.core.extension.extractImageId
import com.tsehla.exiferaser.core.extension.getDisplayName
import com.tsehla.exiferaser.core.extension.isNotNullOrEmpty
import com.tsehla.exiferaser.core.extension.runCatchingResult
import com.tsehla.exiferaser.core.extension.suspendRunCatching
import com.tsehla.exiferaser.core.image.FileNameMode
import com.tsehla.exiferaser.core.image.ImageMetadataSnapshot
import com.tsehla.exiferaser.core.image.ImageProcessingProgress
import com.tsehla.exiferaser.core.image.ImageProcessingStep
import com.tsehla.exiferaser.core.image.ImageProcessingSummary
import com.tsehla.exiferaser.core.image.supportedImageFormats
import com.tsehla.exiferaser.core.provider.CameraFileProvider
import com.tsehla.exiferaser.core.util.FileUtilsKt
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tommygeenexus.exifinterfaceextended.ExifInterfaceExtended
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

import timber.log.Timber

@Singleton
class ImageProcessingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DispatcherIo private val dispatcherIo: CoroutineDispatcher,
) {

    fun removeMetadata(
        protos: List<UserImageSelectionProto>,
        treeUri: Uri,
        displayNameSuffix: String,
        isPreserveOrientationEnabled: Boolean,
        fileNameMode: FileNameMode
    ): Flow<ImageProcessingStep> = flow {
        protos.forEachIndexed { index, proto ->
            emit(
                removeMetaData(
                    proto = proto,
                    treeUri = treeUri,
                    displayNameSuffix = displayNameSuffix,
                    isPreserveOrientationEnabled = isPreserveOrientationEnabled,
                    fileNameMode = fileNameMode,
                    progress = ImageProcessingProgress.calculate(
                        index = index,
                        count = protos.size
                    )
                )
            )
        }
        emit(ImageProcessingStep.FinishedBulk)
    }
        .buffer()
        .flowOn(dispatcherIo)

    private suspend fun removeMetaData(
        proto: UserImageSelectionProto,
        treeUri: Uri,
        displayNameSuffix: String,
        isPreserveOrientationEnabled: Boolean,
        fileNameMode: FileNameMode,
        progress: ImageProcessingProgress
    ): ImageProcessingStep.FinishedSingle {
        var sourceUri = Uri.EMPTY
        var sinkUri = Uri.EMPTY
        var displayName = ""
        var formattedDisplayName = ""
        var extension = ""
        var mimeType = ""
        var imageMetadataSnapshot = ImageMetadataSnapshot()
        var isImageSaved = false
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                sourceUri = proto.image_path.toUri()
                displayName = getDisplayName(sourceUri).getOrThrow()
                mimeType = checkNotNull(context.contentResolver.getType(sourceUri)).also { t ->
                    check(supportedImageFormats.any { it.mimeType == t })
                }
                extension = checkNotNull(
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                )
                val (exifInterfaceExtended, scanResult) = scanImage(sourceUri).getOrThrow()
                imageMetadataSnapshot = scanResult
                val conversionDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
                val lastModifiedDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                Timber.d("Processing image: $displayName, fileNameMode: $fileNameMode, hasMetadata: ${scanResult.isMetadataContained()}")
                if (scanResult.isMetadataContained() || fileNameMode != FileNameMode.IMAGE_ID) {
                    formattedDisplayName = when (fileNameMode) {
                        FileNameMode.CONVERSION_DATE -> {
                            LocalDateTime.now().format(conversionDateTimeFormatter)
                        }
                        FileNameMode.RANDOM -> {
                            val randomName = UUID.randomUUID().toString()
                            if (FileUtilsKt.isValidExtFilename(displayNameSuffix)) {
                                FileUtilsKt.buildValidExtFilename(randomName.plus(displayNameSuffix))
                            } else {
                                randomName
                            }
                        }
                        FileNameMode.LAST_MODIFIED -> {
                            val lastModified = getLastModifiedDate(exifInterfaceExtended)
                            if (lastModified != null) {
                                lastModified.format(lastModifiedDateTimeFormatter)
                            } else {
                                LocalDateTime.now().format(lastModifiedDateTimeFormatter)
                            }
                        }
                        FileNameMode.IMAGE_ID -> {
                            FileUtilsKt.buildValidExtFilename(
                                (sourceUri.extractImageId() ?: displayName)
                                    .plus(
                                        if (sourceUri.authority == CameraFileProvider.getAuthority(context)) {
                                            CameraFileProvider.SUFFIX
                                        } else {
                                            ""
                                        }
                                    )
                                    .plus(
                                        if (FileUtilsKt.isValidExtFilename(displayNameSuffix)) {
                                            displayNameSuffix
                                        } else {
                                            ""
                                        }
                                    )
                            )
                        }
                    }
                    sinkUri = createImage(
                        uri = sourceUri,
                        defaultTreeUri = treeUri,
                        displayName = formattedDisplayName,
                        mimeType = mimeType
                    ).getOrThrow()
                    saveImage(
                        exifInterfaceExtended = exifInterfaceExtended,
                        sourceUri = sourceUri,
                        sinkUri = sinkUri,
                        isPreserveOrientationEnabled = isPreserveOrientationEnabled
                    ).getOrThrow()
                    isImageSaved = true
                }
            }.getOrElse { exception ->
                Timber.e(exception)
            }
            ImageProcessingStep.FinishedSingle(
                imageProcessingSummary = ImageProcessingSummary(
                    displayName = if (isImageSaved) formattedDisplayName else displayName,
                    extension = extension,
                    mimeType = mimeType,
                    isImageSaved = isImageSaved,
                    uri = if (isImageSaved) sinkUri else sourceUri,
                    sourceUri = sourceUri,
                    imageMetadataSnapshot = imageMetadataSnapshot
                ),
                progress = progress
            )
        }
    }

    private suspend fun scanImage(
        sourceUri: Uri
    ): Result<Pair<ExifInterfaceExtended, ImageMetadataSnapshot>> =
        dispatcherIo.runCatchingResult {
            val exifInterfaceExtended: ExifInterfaceExtended
            checkNotNull(context.contentResolver.openInputStream(sourceUri)).use { source ->
                exifInterfaceExtended = ExifInterfaceExtended(source)
            }
            exifInterfaceExtended to ImageMetadataSnapshot(
                isIccProfileContained = exifInterfaceExtended.hasIccProfile(),
                isExifContained = exifInterfaceExtended.hasAttributes(true),
                isPhotoshopImageResourcesContained =
                exifInterfaceExtended.hasPhotoshopImageResources(),
                isXmpContained = exifInterfaceExtended.hasXmp(),
                isExtendedXmpContained = exifInterfaceExtended.hasExtendedXmp()
            )
        }

    private fun getLastModifiedDate(
        exifInterfaceExtended: ExifInterfaceExtended
    ): LocalDateTime? {
        // Try DateTimeOriginal first
        val dateTimeOriginal = exifInterfaceExtended.getAttribute(
            ExifInterfaceExtended.TAG_DATETIME_ORIGINAL
        )
        if (!dateTimeOriginal.isNullOrBlank()) {
            try {
                return LocalDateTime.parse(dateTimeOriginal, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
            } catch (_: Exception) { }
        }
        // Try DateTimeDigitized
        val dateTimeDigitized = exifInterfaceExtended.getAttribute(
            ExifInterfaceExtended.TAG_DATETIME_DIGITIZED
        )
        if (!dateTimeDigitized.isNullOrBlank()) {
            try {
                return LocalDateTime.parse(dateTimeDigitized, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
            } catch (_: Exception) { }
        }
        // No date found in EXIF — return null (caller falls back to current date)
        return null
    }

    private suspend fun createImage(
        uri: Uri,
        defaultTreeUri: Uri = Uri.EMPTY,
        displayName: String,
        mimeType: String
    ): Result<Uri> {
        return dispatcherIo.runCatchingResult {
            if (uri.authority == CameraFileProvider.getAuthority(context)) {
                return@runCatchingResult getFileProviderUri(
                    displayName = displayName,
                    extension = checkNotNull(
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    )
                ).getOrThrow()
            }
            require(uri.scheme == ContentResolver.SCHEME_CONTENT)
            require(displayName.isNotEmpty())
            require(mimeType.isNotEmpty())
            var treeUri: Uri? = null
            if (defaultTreeUri.isNotNullOrEmpty()) {
                val file = DocumentFile.fromTreeUri(context, defaultTreeUri)
                if (file != null && file.isDirectory && file.canWrite()) {
                    treeUri = file.uri
                }
            }
            if (!treeUri.isNotNullOrEmpty()) {
                val file = DocumentFile.fromSingleUri(context, uri)
                if (file != null && file.isFile && file.exists()) {
                    treeUri = file.uri
                }
            }
            checkNotNull(treeUri)
            checkNotNull(
                DocumentsContract.createDocument(
                    context.contentResolver,
                    treeUri,
                    mimeType,
                    displayName
                )
            )
        }
    }

    private suspend fun saveImage(
        exifInterfaceExtended: ExifInterfaceExtended,
        sourceUri: Uri,
        sinkUri: Uri,
        isPreserveOrientationEnabled: Boolean
    ): Result<Unit> =
        dispatcherIo.runCatchingResult {
            checkNotNull(context.contentResolver.openInputStream(sourceUri)).use { source ->
                checkNotNull(context.contentResolver.openOutputStream(sinkUri)).use { sink ->
                    exifInterfaceExtended.saveExclusive(
                        source,
                        sink,
                        isPreserveOrientationEnabled
                    )
                }
            }
        }

    private suspend fun getDisplayName(uri: Uri): Result<String> =
        dispatcherIo.runCatchingResult {
            val name = context.contentResolver.getDisplayName(uri)
            check(name.isNotEmpty())
            name
        }

    suspend fun getLastDocumentPathSegment(uri: Uri): Result<String> =
        dispatcherIo.runCatchingResult {
            checkNotNull(
                DocumentsContract
                    .findDocumentPath(context.contentResolver, uri)
                    ?.path
                    ?.lastOrNull()
            )
        }

    suspend fun getFileProviderUri(displayName: String, extension: String): Result<Uri> =
        dispatcherIo.runCatchingResult {
            CameraFileProvider.getUriForFile(
                context = context,
                displayName = displayName.ifEmpty { UUID.randomUUID().toString() },
                extension = extension.ifEmpty {
                    checkNotNull(
                        MimeTypeMap
                            .getSingleton()
                            .getExtensionFromMimeType(
                                supportedImageFormats.first().mimeType
                            )
                    )
                }
            )
        }

    suspend fun deleteDocuments(uris: List<Uri>): Result<Unit> = dispatcherIo.runCatchingResult {
        uris.forEach { uri ->
            val deleted = when {
                DocumentsContract.isDocumentUri(context, uri) -> {
                    DocumentFile.fromSingleUri(context, uri)?.delete() == true
                }
                uri.scheme == ContentResolver.SCHEME_CONTENT -> {
                    runCatching {
                        context.contentResolver.delete(uri, null, null) > 0
                    }.onFailure { exception ->
                        Timber.e(exception, "Failed to delete %s", uri)
                    }.getOrDefault(false)
                }
                else -> false
            }
            if (!deleted) {
                Timber.e("Failed to delete %s", uri)
            }
        }
    }

    suspend fun deleteOriginals(uris: List<Uri>): OriginalsDeletionOutcome =
        withContext(dispatcherIo) {
            if (uris.isEmpty()) {
                return@withContext OriginalsDeletionOutcome.Deleted
            }
            var unresolvedCount = 0
            val mediaUris = mutableListOf<Uri>()
            uris.forEach { uri ->
                when {
                    uri.authority == CameraFileProvider.getAuthority(context) -> {
                        if (deleteOwnCacheFile(uri)) {
                            Timber.d("Deleted own file %s", uri)
                        } else {
                            Timber.e("Failed to delete own file %s", uri)
                        }
                    }
                    else -> {
                        // MediaStore resolution feeds the R+ trash/consent flow only. On
                        // pre-R it would route a SAF-picked document to a permission-less
                        // contentResolver.delete (SecurityException -> Failed), so keep the
                        // old partition there: documents via DocumentFile (folder grant),
                        // media:// via the direct delete attempt below.
                        val isMediaStoreResolutionEnabled =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        val mediaUri = if (isMediaStoreResolutionEnabled) {
                            resolveMediaStoreUri(uri)
                        } else {
                            null
                        }
                        when {
                            mediaUri != null -> mediaUris.add(mediaUri)
                            DocumentsContract.isDocumentUri(context, uri) -> {
                                if (DocumentFile.fromSingleUri(context, uri)?.delete() == true) {
                                    Timber.d("Deleted document %s", uri)
                                } else {
                                    Timber.e("Failed to delete %s", uri)
                                }
                            }
                            !isMediaStoreResolutionEnabled &&
                                uri.scheme == ContentResolver.SCHEME_CONTENT &&
                                uri.authority == "media" -> mediaUris.add(uri)
                            else -> {
                                unresolvedCount++
                                Timber.w("Original belongs to another app, cannot delete: %s", uri)
                            }
                        }
                    }
                }
            }

            if (mediaUris.isEmpty()) {
                return@withContext if (unresolvedCount > 0) {
                    OriginalsDeletionOutcome.Failed
                } else {
                    OriginalsDeletionOutcome.Deleted
                }
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                val deletedCount = mediaUris.count { mediaUri ->
                    runCatching { context.contentResolver.delete(mediaUri, null, null) > 0 }
                        .onFailure { exception ->
                            Timber.e(exception, "Failed to delete %s", mediaUri)
                        }
                        .getOrDefault(false)
                }
                return@withContext if (unresolvedCount > 0 || deletedCount < mediaUris.size) {
                    OriginalsDeletionOutcome.Failed
                } else {
                    OriginalsDeletionOutcome.Deleted
                }
            }

            val primary = createMediaTrashRequest(mediaUris)
            val result: Result<android.app.PendingIntent> = if (primary.isSuccess) {
                primary
            } else {
                createMediaTrashRequest(mediaUris.map { uri -> mapPickerToMediaUri(uri) })
            }
            result.fold(
                onSuccess = { pendingIntent ->
                    OriginalsDeletionOutcome.ConsentRequired(pendingIntent)
                },
                onFailure = { exception ->
                    Timber.e(exception, "createTrashRequest failed for %s", mediaUris)
                    OriginalsDeletionOutcome.Failed
                },
            )
        }

    private fun deleteOwnCacheFile(uri: Uri): Boolean = runCatching {
        val segments = uri.pathSegments
        check(segments.size >= 2)
        val relativePath = segments.drop(1).joinToString(separator = "/")
        val file = File(context.cacheDir, relativePath)
        check(file.canonicalPath.startsWith(context.cacheDir.canonicalPath + File.separator))
        !file.exists() || file.delete()
    }.getOrElse { exception ->
        Timber.e(exception, "Failed to delete own file %s", uri)
        false
    }

    /**
     * Maps an original URI to a MediaStore item URI (`content://media/.../media/<id>`)
     * so it can be included in a user-consented trash request. Returns null for URIs
     * that are not MediaStore items (e.g. another app's FileProvider) — those cannot
     * be deleted by this app.
     */
    private fun resolveMediaStoreUri(uri: Uri): Uri? {
        if (uri.authority == "media") {
            if (uri.pathSegments.firstOrNull() == "picker") {
                return mapPickerToMediaUri(uri)
            }
            return uri.takeIf { uri.lastPathSegment?.all { char -> char.isDigit() } == true }
        }
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val path = resolveDocumentPath(uri) ?: run {
                Timber.d("Could not resolve filesystem path for %s", uri)
                return null
            }
            return findMediaUriByPath(path).also { mediaUri ->
                if (mediaUri == null) {
                    Timber.d("No MediaStore item for path %s (%s)", path, uri)
                }
            }
        }
        return null
    }

    /**
     * Best-effort filesystem path for a SAF document URI. Primary strategy parses the
     * document ID of the external storage provider (`volume:path/to/file`); fallback
     * normalizes [DocumentsContract.findDocumentPath] segments into an absolute path.
     */
    private fun resolveDocumentPath(uri: Uri): String? {
        if (uri.authority == "com.android.externalstorage.documents") {
            runCatching {
                val documentId = DocumentsContract.getDocumentId(uri)
                val separator = documentId.indexOf(':')
                if (separator > 0) {
                    val volume = documentId.substring(0, separator)
                    val relative = documentId.substring(separator + 1)
                    val base = if (volume.equals("primary", ignoreCase = true)) {
                        "/storage/emulated/0"
                    } else {
                        "/storage/$volume"
                    }
                    return "$base/$relative".takeIf { it.contains('/') }
                }
            }.onFailure { exception ->
                Timber.d(exception, "Failed to parse document id for %s", uri)
            }
        }
        val segments = runCatching {
            DocumentsContract.findDocumentPath(context.contentResolver, uri)?.path
        }.getOrNull() ?: return null
        val joined = segments
            .joinToString(separator = "/") { segment -> segment.trim('/') }
            .trim('/')
        return joined.takeIf { it.isNotEmpty() }?.let { "/$it" }
    }

    private fun findMediaUriByPath(path: String): Uri? = runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(BaseColumns._ID),
            "${MediaStore.MediaColumns.DATA} = ?",
            arrayOf(path),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getString(0),
                )
            } else {
                null
            }
        }
    }.getOrNull()

    private suspend fun createMediaTrashRequest(uris: List<Uri>): Result<android.app.PendingIntent> =
        dispatcherIo.runCatchingResult {
            check(uris.isNotEmpty())
            android.provider.MediaStore.createTrashRequest(context.contentResolver, uris, true)
        }

    private fun mapPickerToMediaUri(uri: Uri): Uri =
        if (uri.authority == "media" && uri.pathSegments.firstOrNull() == "picker") {
            Uri.parse("content://media/external/images/media/${uri.lastPathSegment}")
        } else {
            uri
        }

    suspend fun moveDocuments(
        sourceUris: List<Uri>,
        destinationTreeUri: Uri
    ): Result<List<Pair<Uri, Uri>>> = dispatcherIo.runCatchingResult {
        val destinationTree = checkNotNull(DocumentFile.fromTreeUri(context, destinationTreeUri))
        val moved = mutableListOf<Pair<Uri, Uri>>()
        sourceUris.forEach { sourceUri ->
            runCatching {
                val newUri = moveOrCopyDocument(sourceUri = sourceUri, destinationTree = destinationTree)
                moved.add(sourceUri to newUri)
            }.onFailure { exception ->
                Timber.e(exception, "Failed to move %s", sourceUri)
            }
        }
        moved
    }

    private suspend fun moveOrCopyDocument(sourceUri: Uri, destinationTree: DocumentFile): Uri {
        if (DocumentsContract.isDocumentUri(context, sourceUri)) {
            runCatching {
                val sourcePath = DocumentsContract
                    .findDocumentPath(context.contentResolver, sourceUri)
                    ?.path
                    .orEmpty()
                val parentPath = sourcePath.dropLast(1)
                if (parentPath.isNotEmpty()) {
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(
                        sourceUri,
                        parentPath.joinToString(separator = "/")
                    )
                    DocumentsContract.moveDocument(
                        context.contentResolver,
                        sourceUri,
                        parentUri,
                        destinationTree.uri
                    )
                } else {
                    null
                }
            }.getOrNull()?.let { movedUri ->
                return movedUri
            }
        }
        val mimeType = checkNotNull(context.contentResolver.getType(sourceUri))
        val displayName = getDisplayName(sourceUri).getOrThrow()
        val sinkUri = checkNotNull(
            DocumentsContract.createDocument(
                context.contentResolver,
                destinationTree.uri,
                mimeType,
                displayName
            )
        )
        checkNotNull(context.contentResolver.openInputStream(sourceUri)).use { source ->
            checkNotNull(context.contentResolver.openOutputStream(sinkUri)).use { sink ->
                source.copyTo(sink)
            }
        }
        if (DocumentsContract.isDocumentUri(context, sourceUri)) {
            if (DocumentFile.fromSingleUri(context, sourceUri)?.delete() != true) {
                Timber.e("Failed to delete moved source %s", sourceUri)
            }
        }
        return sinkUri
    }
}
