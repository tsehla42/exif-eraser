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

package com.tsehla.exiferaser.imageProcessing.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tsehla.exiferaser.UserImageSelectionProto
import com.tsehla.exiferaser.core.extension.addOrShift
import com.tsehla.exiferaser.core.extension.isNotEmpty
import com.tsehla.exiferaser.core.image.ImageProcessingStep
import com.tsehla.exiferaser.core.util.IMAGE_PROCESSING_REPORT_IMAGES_MAX
import com.tsehla.exiferaser.imageProcessing.data.ImageProcessingRepository
import com.tsehla.exiferaser.imageProcessing.data.OriginalsDeletionOutcome
import com.tsehla.exiferaser.main.data.SelectionRepository
import com.tsehla.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class ImageProcessingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageProcessingRepository: ImageProcessingRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel(),
    ContainerHost<ImageProcessingState, ImageProcessingSideEffect> {

    override val container = container<ImageProcessingState, ImageProcessingSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = ImageProcessingState(),
        onCreate = {
            if (state.imageImageProcessingStep !is ImageProcessingStep.FinishedBulk) {
                readSelection(fromIndex = state.imagesProcessedCount)
            }
        }
    )

    fun handleImageProcessingDetails() = intent {
        postSideEffect(
            ImageProcessingSideEffect.Navigate.ToImageProcessingDetails(
                imageProcessingSummaries = state.imageProcessingSummaries.toTypedArray()
            )
        )
    }

    fun handleUserImagesSelection(protos: List<UserImageSelectionProto>, treeUri: Uri) = intent {
        val deleteConvertedEnabled = settingsRepository.isDeleteConvertedEnabled()
        val deleteOriginalsEnabled = settingsRepository.isAutoDeleteEnabled()
        reduce {
            state.copy(
                isDeleteConvertedChecked = deleteConvertedEnabled,
                isDeleteOriginalsChecked = deleteOriginalsEnabled,
            )
        }
        imageProcessingRepository.removeMetadata(
            protos = protos,
            treeUri = treeUri,
            displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix(),
            isPreserveOrientationEnabled = settingsRepository.isPreserveOrientationEnabled(),
            fileNameMode = settingsRepository.getFileNameMode()
        ).collect { result ->
            reduce {
                when (result) {
                    is ImageProcessingStep.Idle,
                    is ImageProcessingStep.FinishedBulk -> {
                        state.copy(imageImageProcessingStep = result)
                    }
                    is ImageProcessingStep.FinishedSingle -> {
                        val imageProcessingSummaries = state
                            .imageProcessingSummaries
                            .toMutableList()
                            .addOrShift(
                                element = result.imageProcessingSummary,
                                shiftAtSize = IMAGE_PROCESSING_REPORT_IMAGES_MAX
                            )
                        val uris = state
                            .uris
                            .toMutableList()
                            .addOrShift(
                                element = result.imageProcessingSummary.uri,
                                shiftAtSize = IMAGE_PROCESSING_REPORT_IMAGES_MAX
                            )
                        val isMetadataContained = result
                            .imageProcessingSummary
                            .imageMetadataSnapshot
                            .isMetadataContained()
                        val imagesWithMetadataCount = state
                            .imagesWithMetadataCount
                            .plus(if (isMetadataContained) 1 else 0)
                        val imagesSavedCount = state
                            .imagesSavedCount
                            .plus(if (result.imageProcessingSummary.isImageSaved) 1 else 0)
                        val summary = result.imageProcessingSummary
                        val savedUris = if (summary.isImageSaved) {
                            state.savedUris + summary.uri
                        } else {
                            state.savedUris
                        }
                        val originalUris = if (summary.isImageSaved && summary.sourceUri.isNotEmpty()) {
                            state.originalUris + summary.sourceUri
                        } else {
                            state.originalUris
                        }
                        state.copy(
                            imageImageProcessingStep = result,
                            imageProcessingSummaries = imageProcessingSummaries,
                            uris = uris,
                            savedUris = savedUris,
                            originalUris = originalUris,
                            imagesWithMetadataCount = imagesWithMetadataCount,
                            imagesSavedCount = imagesSavedCount,
                            imagesProcessedCount = state.imagesProcessedCount.inc(),
                            progress = result.progress
                        )
                    }
                }
            }
            if (result is ImageProcessingStep.FinishedBulk &&
                settingsRepository.isShareByDefaultEnabled()
            ) {
                shareImages()
            }
        }
    }

    fun handleUnsupportedSelection() = intent {
        reduce {
            state.copy(imageImageProcessingStep = ImageProcessingStep.FinishedBulk)
        }
    }

    fun readSelection(
        fromIndex: Int,
        treeUri: Uri = Uri.parse(savedStateHandle["navArgImageSavePath"] ?: "")
    ) = intent {
        val protos = selectionRepository.getSelection(fromIndex)
        postSideEffect(
            if (protos.isEmpty()) {
                ImageProcessingSideEffect.Handle.UnsupportedSelection
            } else {
                ImageProcessingSideEffect.Handle.UserImagesSelection(protos, treeUri)
            }
        )
    }

    /**
     * "Converted" means EXIF was actually cleared from a saved copy: the source
     * contained metadata AND the cleaned copy exists. Files without EXIF are never
     * part of the converted set, even when non-IMAGE_ID naming copied them — the
     * dialog must offer a choice whenever a batch mixes both kinds.
     */
    private fun shareableUris(convertedOnly: Boolean): List<Uri> = container
        .stateFlow
        .value
        .imageProcessingSummaries
        .filter { summary ->
            if (convertedOnly) {
                summary.imageMetadataSnapshot.isMetadataContained() && summary.isImageSaved
            } else {
                true
            }
        }
        .map { summary -> summary.uri }
        .filter { uri -> uri != Uri.EMPTY }

    fun shareImages() = intent {
        val converted = shareableUris(convertedOnly = true)
        val all = shareableUris(convertedOnly = false)
        when {
            all.isEmpty() -> Unit
            converted.isNotEmpty() && converted.size < all.size -> {
                reduce { state.copy(isShareDialogVisible = true) }
            }
            else -> postSideEffect(ImageProcessingSideEffect.ShareImages(ArrayList(all)))
        }
    }

    fun shareImagesFromDialog(convertedOnly: Boolean) = intent {
        reduce { state.copy(isShareDialogVisible = false) }
        shareableUris(convertedOnly)
            .takeIf { uris -> uris.isNotEmpty() }
            ?.let { uris -> postSideEffect(ImageProcessingSideEffect.ShareImages(ArrayList(uris))) }
    }

    fun dismissShareDialog() = intent {
        reduce { state.copy(isShareDialogVisible = false) }
    }

    fun toggleDeleteConverted() = intent {
        reduce { state.copy(isDeleteConvertedChecked = !state.isDeleteConvertedChecked) }
    }

    fun toggleDeleteOriginals() = intent {
        reduce { state.copy(isDeleteOriginalsChecked = !state.isDeleteOriginalsChecked) }
    }

    fun handleConfirmClose() = intent {
        val convertedUris = if (state.isDeleteConvertedChecked) {
            state.savedUris
        } else {
            emptyList()
        }
        val originalUrisToDelete = if (state.isDeleteOriginalsChecked) {
            state.originalUris
        } else {
            emptyList()
        }
        if (convertedUris.isNotEmpty()) {
            imageProcessingRepository.deleteDocuments(convertedUris)
        }
        if (originalUrisToDelete.isNotEmpty()) {
            when (val outcome = imageProcessingRepository.deleteOriginals(originalUrisToDelete)) {
                is OriginalsDeletionOutcome.ConsentRequired -> {
                    postSideEffect(
                        ImageProcessingSideEffect.RequestMediaDeletion(outcome.pendingIntent)
                    )
                    return@intent
                }
                OriginalsDeletionOutcome.Deleted -> Unit
                OriginalsDeletionOutcome.Failed -> {
                    postSideEffect(ImageProcessingSideEffect.OriginalsDeletionFailed)
                    return@intent
                }
            }
        }
        postSideEffect(ImageProcessingSideEffect.Navigate.Close)
    }

    fun handleDeletionRequestResult() = intent {
        postSideEffect(ImageProcessingSideEffect.Navigate.Close)
    }

    fun saveElsewhere(destinationTreeUri: Uri) = intent {
        if (state.savedUris.isEmpty() || destinationTreeUri == Uri.EMPTY) {
            return@intent
        }
        val moves = imageProcessingRepository
            .moveDocuments(state.savedUris, destinationTreeUri)
            .getOrElse { emptyList() }
        moves.forEach { (oldUri, newUri) ->
            reduce {
                state.copy(
                    savedUris = state.savedUris.map { uri -> if (uri == oldUri) newUri else uri },
                    uris = state.uris.map { uri -> if (uri == oldUri) newUri else uri },
                    imageProcessingSummaries = state.imageProcessingSummaries.map { summary ->
                        if (summary.uri == oldUri) summary.copy(uri = newUri) else summary
                    },
                )
            }
        }
    }
}
