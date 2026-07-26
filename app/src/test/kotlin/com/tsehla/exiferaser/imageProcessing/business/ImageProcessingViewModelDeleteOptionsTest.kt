package com.tsehla.exiferaser.imageProcessing.business

import android.app.PendingIntent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tsehla.exiferaser.UserImageSelectionProto
import com.tsehla.exiferaser.core.image.FileNameMode
import com.tsehla.exiferaser.core.image.ImageProcessingProgress
import com.tsehla.exiferaser.core.image.ImageProcessingStep
import com.tsehla.exiferaser.core.image.ImageProcessingSummary
import com.tsehla.exiferaser.imageProcessing.data.ImageProcessingRepository
import com.tsehla.exiferaser.imageProcessing.data.OriginalsDeletionOutcome
import com.tsehla.exiferaser.main.data.SelectionRepository
import com.tsehla.exiferaser.settings.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class ImageProcessingViewModelDeleteOptionsTest {

    private val imageProcessingRepository = mockk<ImageProcessingRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    private val treeUri: Uri = Uri.parse("content://tree/primary%3ADownload")
    private val sourceUri: Uri = Uri.parse("content://media/images/1")
    private val convertedUri: Uri = Uri.parse("content://tree/primary%3ADownload/converted.jpg")

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
    ) = ImageProcessingStep.FinishedSingle(
        imageProcessingSummary = ImageProcessingSummary(
            displayName = displayName,
            uri = converted,
            sourceUri = source,
            isImageSaved = true,
        ),
        progress = ImageProcessingProgress.calculate(index = 0, count = 1),
    )

    private val proto = UserImageSelectionProto(image_path = sourceUri.toString())

    @Test
    fun `delete checkboxes initialize from settings after processing finishes`() = runTest {
        stubCommon(deleteConvertedEnabled = true, autoDeleteEnabled = true)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        val state = awaitProcessingFinished(viewModel)

        assertTrue(state.isDeleteConvertedChecked)
        assertTrue(state.isDeleteOriginalsChecked)
    }

    @Test
    fun `toggling delete checkbox is per run and does not write settings`() = runTest {
        stubCommon(deleteConvertedEnabled = true, autoDeleteEnabled = false)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        assertTrue(awaitProcessingFinished(viewModel).isDeleteConvertedChecked)

        viewModel.toggleDeleteConverted()
        val state = awaitState(viewModel) { !it.isDeleteConvertedChecked }
        assertFalse(state.isDeleteConvertedChecked)

        coVerify(exactly = 0) { settingsRepository.putDeleteConverted(any()) }
        coVerify(exactly = 0) { settingsRepository.putAutoDelete(any()) }
    }

    @Test
    fun `confirm deletes converted files when checked`() = runTest {
        stubCommon(deleteConvertedEnabled = true, autoDeleteEnabled = false)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.handleConfirmClose()
            // Side effects may be preceded by the pending UnsupportedSelection effect
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.Navigate.Close) {
                received = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            imageProcessingRepository.deleteDocuments(listOf(convertedUri))
        }
    }

    @Test
    fun `confirm deletes original files when checked`() = runTest {
        stubCommon(deleteConvertedEnabled = false, autoDeleteEnabled = true)
        val unsavedSource = Uri.parse("content://media/images/2")
        stubProcessingFlow(
            savedStep(),
            ImageProcessingStep.FinishedSingle(
                imageProcessingSummary = ImageProcessingSummary(
                    displayName = "no-exif",
                    uri = unsavedSource,
                    sourceUri = unsavedSource,
                    isImageSaved = false,
                ),
                progress = ImageProcessingProgress.calculate(index = 1, count = 2),
            ),
            ImageProcessingStep.FinishedBulk,
        )

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.handleConfirmClose()
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.Navigate.Close) {
                received = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            imageProcessingRepository.deleteOriginals(listOf(sourceUri))
        }
    }

    @Test
    fun `originals needing consent show system dialog before closing`() = runTest {
        stubCommon(deleteConvertedEnabled = true, autoDeleteEnabled = true)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)
        val pendingIntent = mockk<PendingIntent>()
        coEvery {
            imageProcessingRepository.deleteOriginals(listOf(sourceUri))
        } returns OriginalsDeletionOutcome.ConsentRequired(pendingIntent)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.handleConfirmClose()
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.RequestMediaDeletion) {
                received = awaitItem()
            }
            assertEquals(pendingIntent, received.pendingIntent)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) {
            imageProcessingRepository.deleteDocuments(listOf(convertedUri))
        }

        viewModel.container.sideEffectFlow.test {
            viewModel.handleDeletionRequestResult()
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.Navigate.Close) {
                received = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed originals deletion shows an error instead of closing silently`() = runTest {
        stubCommon(deleteConvertedEnabled = false, autoDeleteEnabled = true)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)
        coEvery {
            imageProcessingRepository.deleteOriginals(listOf(sourceUri))
        } returns OriginalsDeletionOutcome.Failed

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.handleConfirmClose()
            var received: ImageProcessingSideEffect? = null
            while (
                received !is ImageProcessingSideEffect.OriginalsDeletionFailed &&
                received !is ImageProcessingSideEffect.Navigate.Close
            ) {
                received = awaitItem()
            }
            assertTrue(
                "expected OriginalsDeletionFailed but was $received",
                received is ImageProcessingSideEffect.OriginalsDeletionFailed,
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            imageProcessingRepository.deleteOriginals(listOf(sourceUri))
        }
    }

    @Test
    fun `confirm with no checkboxes checked closes without deleting`() = runTest {
        stubCommon(deleteConvertedEnabled = false, autoDeleteEnabled = false)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)

        viewModel.container.sideEffectFlow.test {
            viewModel.handleConfirmClose()
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.Navigate.Close) {
                received = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { imageProcessingRepository.deleteDocuments(any()) }
    }

    @Test
    fun `sharing never deletes files`() = runTest {
        stubCommon(deleteConvertedEnabled = true, autoDeleteEnabled = true)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        val state = awaitProcessingFinished(viewModel)
        assertEquals(listOf(convertedUri), state.savedUris)
        assertEquals(listOf(sourceUri), state.originalUris)

        viewModel.container.sideEffectFlow.test {
            viewModel.shareImages()
            var received: ImageProcessingSideEffect? = null
            while (received !is ImageProcessingSideEffect.ShareImages) {
                received = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { imageProcessingRepository.deleteDocuments(any()) }
    }

    @Test
    fun `save elsewhere moves saved files to picked location`() = runTest {
        stubCommon(deleteConvertedEnabled = false, autoDeleteEnabled = false)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)
        val destination = Uri.parse("content://tree/primary%3ADocuments")
        val movedUri = Uri.parse("content://tree/primary%3ADocuments/converted.jpg")
        coEvery {
            imageProcessingRepository.moveDocuments(listOf(convertedUri), destination)
        } returns Result.success(listOf(convertedUri to movedUri))

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)
        viewModel.saveElsewhere(destination)

        val state = awaitState(viewModel) {
            it.savedUris == listOf(movedUri)
        }
        assertEquals(movedUri, state.imageProcessingSummaries.first().uri)
        assertEquals(listOf(sourceUri), state.originalUris)
        coVerify(exactly = 1) {
            imageProcessingRepository.moveDocuments(listOf(convertedUri), destination)
        }
    }

    @Test
    fun `failed move keeps files tracked at their current location`() = runTest {
        stubCommon(deleteConvertedEnabled = false, autoDeleteEnabled = false)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)
        val destination = Uri.parse("content://tree/primary%3ADocuments")
        val moveStarted = java.util.concurrent.CountDownLatch(1)
        coEvery {
            imageProcessingRepository.moveDocuments(listOf(convertedUri), destination)
        } answers {
            moveStarted.countDown()
            Result.failure(IllegalStateException("move failed"))
        }

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)
        viewModel.saveElsewhere(destination)

        assertTrue(moveStarted.await(5, java.util.concurrent.TimeUnit.SECONDS))
        Thread.sleep(200)
        val state = viewModel.container.stateFlow.value
        assertEquals(listOf(convertedUri), state.savedUris)
        assertEquals(convertedUri, state.imageProcessingSummaries.first().uri)
    }

    @Test
    fun `processing does not delete anything on its own`() = runTest {
        stubCommon(deleteConvertedEnabled = true, autoDeleteEnabled = true)
        stubProcessingFlow(savedStep(), ImageProcessingStep.FinishedBulk)

        val viewModel = createViewModel()
        viewModel.handleUserImagesSelection(listOf(proto), treeUri)
        awaitProcessingFinished(viewModel)
        Thread.sleep(200)

        coVerify(exactly = 0) { imageProcessingRepository.deleteDocuments(any()) }
    }

    private companion object {
        const val STATE_AWAIT_TIMEOUT_MS = 5_000L
    }
}
