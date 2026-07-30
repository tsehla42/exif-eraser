/*
 * Copyright (c) 2024-2026, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.none.tom.exiferaser.core.extension

import android.content.ContentResolver
import android.net.Uri
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
                if (baseName.isNotEmpty() && !baseName.all { it.isDigit() }) {
                    return@withContext baseName
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
                    } else {
                        return@withContext baseNameWithoutExt
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get display name for URI: $uri")
        }

        return@withContext ""
    }
}
