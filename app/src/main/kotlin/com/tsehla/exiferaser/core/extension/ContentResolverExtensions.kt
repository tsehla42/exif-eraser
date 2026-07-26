package com.tsehla.exiferaser.core.extension

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import timber.log.Timber

private val DOCUMENT_AUTHORITIES = setOf(
    "com.android.externalstorage.documents",
    "com.android.providers.downloads.documents",
    "com.android.providers.media.documents"
)

suspend fun ContentResolver.getDisplayName(uri: Uri): String {
    val resolver = this
    return withContext(Dispatchers.IO) {
        try {
            val authority = uri.authority
            if (authority in DOCUMENT_AUTHORITIES) {
                val documentId = DocumentsContract.getDocumentId(uri)
                Timber.d("Document ID: $documentId for URI: $uri")
                val path = if (documentId.contains(":")) {
                    documentId.substringAfter(":")
                } else {
                    documentId
                }
                val baseName = FilenameUtils.getBaseName(path)
                Timber.d("Extracted base name: $baseName from path: $path")
                val hasMeaningfulBaseName = baseName.isNotEmpty() && !baseName.all { it.isDigit() }
                if (hasMeaningfulBaseName) {
                    return@withContext baseName
                }
            }

            val mediaId = extractMediaId(uri)
            if (mediaId != null) {
                val realName = queryDisplayNameById(mediaId)
                if (realName != null) {
                    Timber.d("Got real display name from MediaStore by ID $mediaId: $realName")
                    return@withContext realName
                }
            }

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)
                    val displayName = if (displayNameIndex >= 0) {
                        cursor.getString(displayNameIndex) ?: ""
                    } else {
                        ""
                    }

                    Timber.d("MediaStore display name: $displayName for URI: $uri")

                    val baseNameWithoutExt = FilenameUtils.getBaseName(displayName)
                    val isNumericId = baseNameWithoutExt.all { it.isDigit() }

                    if (isNumericId) {
                        val dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                        if (dataIndex >= 0) {
                            val filePath = cursor.getString(dataIndex) ?: ""
                            if (filePath.isNotEmpty()) {
                                val actualFileName = FilenameUtils.getName(filePath)
                                Timber.d("Got actual filename from file path: $actualFileName")
                                val actualBaseName = FilenameUtils.getBaseName(actualFileName)
                                if (!actualBaseName.all { it.isDigit() }) {
                                    return@withContext actualBaseName
                                }
                            }
                        }
                    }

                    return@withContext baseNameWithoutExt
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get display name for URI: $uri")
        }

        return@withContext ""
    }
}

private fun extractMediaId(uri: Uri): String? {
    val uriString = uri.toString()

    if (uriString.contains("media/picker") || uriString.contains("media/external")) {
        val lastSegment = uri.lastPathSegment
        if (lastSegment != null && lastSegment.all { it.isDigit() }) {
            return lastSegment
        }
    }

    val pathSegments = uri.pathSegments
    for (i in pathSegments.size - 1 downTo 0) {
        val segment = pathSegments[i]
        if (segment.all { it.isDigit() } && segment.isNotEmpty()) {
            return segment
        }
    }

    return null
}

private suspend fun ContentResolver.queryDisplayNameById(mediaId: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val selection = "${MediaStore.Images.Media._ID} = ?"
            val selectionArgs = arrayOf(mediaId)

            query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex) ?: ""
                        val baseName = FilenameUtils.getBaseName(name)
                        Timber.d("queryDisplayNameById: ID=$mediaId, raw=$name, baseName=$baseName")
                        if (baseName.isNotEmpty() && !baseName.all { it.isDigit() }) {
                            return@withContext baseName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query display name for media ID: $mediaId")
        }
        null
    }
}
