/*
 * Copyright (c) 2024-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.tsehla.exiferaser.main.data

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.tsehla.exiferaser.core.di.DispatcherIo
import com.tsehla.exiferaser.core.extension.supportedImageUrisToList
import com.tsehla.exiferaser.core.extension.runCatchingResult
import com.tsehla.exiferaser.core.image.supportedMimeTypes
import com.tsehla.exiferaser.core.provider.CameraFileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class MainRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DispatcherIo private val dispatcherIo: CoroutineDispatcher,
) {

    suspend fun deleteCameraImages(): Result<Unit> = dispatcherIo.runCatchingResult {
        check(CameraFileProvider.getRootDirectory(context).deleteRecursively())
    }

    suspend fun getChildDocuments(treeUri: Uri?): Result<List<Uri>> = dispatcherIo.runCatchingResult {
        val childDocumentsUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        checkNotNull(
            context.contentResolver.query(
                childDocumentsUri,
                null,
                null,
                null,
                null
            )
        ).use { cursor ->
            val uris = mutableListOf<Uri>()
            while (cursor.moveToNext()) {
                val columnName = DocumentsContract.Document.COLUMN_DOCUMENT_ID
                val id = cursor.getString(cursor.getColumnIndexOrThrow(columnName))
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                val mimeType = context.contentResolver.getType(uri).orEmpty()
                    if (supportedMimeTypes.contains(mimeType)) {
                    uris.add(uri)
                }
            }
            check(uris.isNotEmpty())
            uris
        }
    }

    suspend fun getPrimaryClipImageUris(): Result<List<Uri>> = dispatcherIo.runCatchingResult {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val primaryClip = cm.primaryClip
        check(cm.hasPrimaryClip())
        checkNotNull(primaryClip)
        val uris = primaryClip.supportedImageUrisToList()
        check(uris.isNotEmpty())
        uris
    }

    suspend fun getFileProviderUri(): Result<Uri> = dispatcherIo.runCatchingResult {
        CameraFileProvider.getUriForFile(
            context = context,
            displayName = UUID.randomUUID().toString(),
            extension = checkNotNull(
                MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(
                        supportedMimeTypes.first()
                    )
            )
        )
    }
}
