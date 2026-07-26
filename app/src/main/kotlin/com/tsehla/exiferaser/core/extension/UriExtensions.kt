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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tsehla.exiferaser.core.extension

import android.net.Uri
import android.provider.DocumentsContract
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun Uri.isNotEmpty() = this != Uri.EMPTY

@OptIn(ExperimentalContracts::class)
fun Uri?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && isNotEmpty()
}

/**
 * Numeric internal ID (photo-picker / MediaStore ID) for URIs that carry one:
 * media-authority URIs (photo picker selections and shared MediaStore items) use
 * their last path segment; document-provider URIs use the numeric part of their
 * document ID (`images_media:12636` -> `12636`).
 *
 * Returns null when no such ID exists (camera FileProvider, SAF file paths,
 * opaque third-party providers) so callers can fall back to the display name.
 */
fun Uri.extractImageId(): String? {
    if (authority == "media") {
        return lastPathSegment?.takeIf { segment ->
            segment.isNotEmpty() && segment.all { char -> char.isDigit() }
        }
    }
    val uriAuthority = authority ?: return null
    if (uriAuthority.endsWith(".documents")) {
        val documentId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull()
            ?: return null
        val idPart = documentId.substringAfter(':', documentId)
        return idPart.takeIf { it.isNotEmpty() && it.all { char -> char.isDigit() } }
    }
    return null
}
