package com.tsehla.exiferaser.imageProcessing.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tsehla.exiferaser.UserImageSelectionProto
import com.tsehla.exiferaser.core.image.FileNameMode
import com.tsehla.exiferaser.core.image.ImageMetadataSnapshot
import com.tsehla.exiferaser.core.image.ImageProcessingProgress
import com.tsehla.exiferaser.core.image.ImageProcessingStep
import com.tsehla.exiferaser.core.image.ImageProcessingSummary
import com.tsehla.exiferaser.imageProcessing.data.ImageProcessingRepository
import com.tsehla.exiferaser.imageProcessing.data.OriginalsDeletionOutcome
import com.tsehla.exiferaser.main.data.SelectionRepository
import com.tsehla.exiferaser.settings.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImageProcessingViewModelShareTest {

    private val imageProcessingRepository = mockk<ImageProcessingRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    private val treeUri: Uri = Uri.parse("content://tree/primary%3ADownload")
    private val sourceUri: Uri = Uri.parse("content://media/images/1")
    private val convertedUri: Uri = Uri.parse("content://tree/primary%3ADownload/converted.jpg")
    private val unsavedSource: Uri = Uri.parse("content://media/images/2")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ImageProcessingViewModel(
        SavedStateHandle(),
        imageProcessingRepository,
        selectionRepository,
        settingsRepository,
    )

    /**
     * Orbit intents run on a background worker, so state must be awaited
     * with real time instead of read synchronously.
     */
    private fun awaitState(
        viewModel: ImageProcessingViewModel,
        predicate: (ImageProcessingState) -> Boolean,
    ): ImageProcessingState {
        val deadline = System.currentTimeMillis() + STATE_AWAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val state = viewModel.container.stateFlow.value
            if (predicate(state)) {
                return state
            }
            Thread.sleep(20)
        }
        return viewModel.container.stateFlow.value
    }

    private fun awaitProcessingFinished(viewModel: ImageProcessingViewModel): ImageProcessingState =
        awaitState(viewModel) { it.imageImageProcessingStep is ImageProcessingStep.FinishedBulk }

    private fun stubCommon(
        deleteConvertedEnabled: Boolean = false,
        autoDeleteEnabled: Boolean = false,
    ) {
        coEvery { selectionRepository.getSelection(0) } returns emptyList()
        coEvery { settingsRepository.getDefaultDisplayNameSuffix() } returns ""
        coEvery { settingsRepository.isPreserveOrientationEnabled() } returns false
        coEvery { settingsRepository.getFileNameMode() } returns FileNameMode.IMAGE_ID
        coEvery { settingsRepository.isShareByDefaultEnabled() } returns false
        coEvery { settingsRepository.isDeleteConvertedEnabled() } returns deleteConvertedEnabled
        coEvery { settingsRepository.isAutoDeleteEnabled() } returns autoDeleteEnabled
        coEvery { imageProcessingRepository.deleteDocuments(any()) } returns Result.success(Unit)
        coEvery { imageProcessingRepository.deleteOriginals(any()) } returns OriginalsDeletionOutcome.Deleted
    }

    private fun stubProcessingFlow(
        vararg steps: ImageProcessingStep,
    ) {
        every {
            imageProcessingRepository.removeMetadata(any(), any(), any(), any(), any())
        } returns flowOf(*steps)
    }

    private fun savedStep(
        displayName: String = "converted",
        source: Uri = sourceUri,
        converted: Uri = convertedUri,
        hasExif: Boolean = true,
    ) = ImageProcessingStep.FinishedSingle(
        imageProcessingSummary = ImageProcessingSummary(
            displayName = displayName,
            uri = converted,
            sourceUri = source,
            isImageSaved = true,
            imageMetadataSnapshot = ImageMetadataSnapshot(isExifContained = hasExif),
        ),
        progress = ImageProcessingProgress.calculate(index = 0, count = 1),
    )

    private fun unsavedStep(source: Uri) = ImageProcessingStep.FinishedSingle(
        imageProcessingSummary = ImageProcessingSummary(
            displayName = "no-exif",
            uri = source,
            sourceUri = source,
            isImageSaved = false,
        ),
        progress = ImageProcessingProgress.calculate(index = 1, count = 2),
    )

    private val proto = UserImageSelectionProto(image_path = sourceUri.toString())

    @Test
    fun `batch fully converted shares directly without dialog`() = runTest {
        stubCommon(); stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)
        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)
        viewModel.container.sideEffectFlow.test {
            viewModel.shareImages()
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.ShareImages) received = awaitItem()
            assertEquals(listOf(convertedUri), received.imageUris)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.container.stateFlow.value.isShareDialogVisible)
    }

    @Test
    fun `mixed batch opens dialog instead of sharing`() = runTest {
        stubCommon()
        stubProcessingFlow(savedStep(), unsavedStep(unsavedSource), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.shareImages()
        assertTrue(awaitState(viewModel) { it.isShareDialogVisible }.isShareDialogVisible)
    }

    @Test
    fun `share all from dialog includes unconverted originals and skips empty uris`() = runTest {
        stubCommon()
        stubProcessingFlow(
            savedStep(),
            unsavedStep(unsavedSource),
            ImageProcessingStep.FinishedSingle(
                imageProcessingSummary = ImageProcessingSummary(
                    uri = Uri.EMPTY,
                    sourceUri = Uri.EMPTY,
                    isImageSaved = false,
                ),
                progress = ImageProcessingProgress.calculate(index = 2, count = 3),
            ),
            ImageProcessingStep.FinishedBulk,
        )

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.shareImages()
            awaitState(viewModel) { it.isShareDialogVisible }
            viewModel.shareImagesFromDialog(convertedOnly = false)
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.ShareImages) received = awaitItem()
            assertEquals(listOf(convertedUri, unsavedSource), received.imageUris)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `share only converted from dialog sends just converted uris`() = runTest {
        stubCommon()
        stubProcessingFlow(savedStep(), unsavedStep(unsavedSource), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.shareImages()
            awaitState(viewModel) { it.isShareDialogVisible }
            viewModel.shareImagesFromDialog(convertedOnly = true)
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.ShareImages) received = awaitItem()
            assertEquals(listOf(convertedUri), received.imageUris)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.container.stateFlow.value.isShareDialogVisible)
    }

    @Test
    fun `batch with nothing converted shares originals directly without dialog`() = runTest {
        stubCommon()
        stubProcessingFlow(unsavedStep(unsavedSource), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.shareImages()
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.ShareImages) received = awaitItem()
            assertEquals(listOf(unsavedSource), received.imageUris)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.container.stateFlow.value.isShareDialogVisible)
    }

    @Test
    fun `auto share with mixed batch opens dialog instead of sharing`() = runTest {
        stubCommon()
        coEvery { settingsRepository.isShareByDefaultEnabled() } returns true
        stubProcessingFlow(savedStep(), unsavedStep(unsavedSource), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        assertTrue(awaitState(viewModel) { it.isShareDialogVisible }.isShareDialogVisible)
    }

    @Test
    fun `dismiss share dialog hides it and shares nothing`() = runTest {
        stubCommon()
        stubProcessingFlow(savedStep(), unsavedStep(unsavedSource), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.shareImages()
        assertTrue(awaitState(viewModel) { it.isShareDialogVisible }.isShareDialogVisible)
        viewModel.dismissShareDialog()
        assertFalse(awaitState(viewModel) { !it.isShareDialogVisible }.isShareDialogVisible)
    }

    @Test
    fun `mixed batch where every file was saved still opens dialog`() = runTest {
        // Non-IMAGE_ID naming copies no-EXIF files too: both summaries have
        // isImageSaved = true, only one had EXIF. The dialog must still appear.
        val noExifSource = Uri.parse("content://media/images/3")
        val noExifConverted = Uri.parse("content://tree/primary%3ADownload/noexif_copy.jpg")
        stubCommon()
        stubProcessingFlow(
            savedStep(),
            savedStep(
                displayName = "no-exif-copy",
                source = noExifSource,
                converted = noExifConverted,
                hasExif = false,
            ),
            ImageProcessingStep.FinishedBulk,
        )

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.shareImages()
        assertTrue(awaitState(viewModel) { it.isShareDialogVisible }.isShareDialogVisible)
    }

    private companion object {
        const val STATE_AWAIT_TIMEOUT_MS = 5_000L
    }
}
