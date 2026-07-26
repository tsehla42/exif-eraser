package com.tsehla.exiferaser.main.data

import android.net.Uri
import com.tsehla.exiferaser.settings.data.SettingsRepository
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentCoordinatorTest {

    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val shareIntentHolder = ShareIntentHolder()

    private fun createCoordinator() = ShareIntentCoordinator(
        shareIntentHolder,
        selectionRepository,
        settingsRepository,
    )

    @Test
    fun `pending share is exposed from holder`() {
        val uri = Uri.parse("content://media/images/1")
        shareIntentHolder.submit(listOf(uri))

        assertEquals(listOf(uri), createCoordinator().pendingShare.value)
    }

    @Test
    fun `shared image with valid default save path navigates to processing`() = runTest {
        val uri = Uri.parse("content://media/images/1")
        val savePath = Uri.parse("content://tree/primary%3ADownload")
        coEvery { selectionRepository.putSelection(uri, false) } returns Result.success(Unit)
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.success(savePath)

        val outcome = createCoordinator().handlePendingShare(listOf(uri))

        assertEquals(ShareOutcome.NavigateToProcessing(savePath), outcome)
        assertNull(shareIntentHolder.uris.value)
        coVerify(exactly = 1) { selectionRepository.putSelection(uri, false) }
    }

    @Test
    fun `shared images with valid default save path navigate to processing`() = runTest {
        val uris = listOf(
            Uri.parse("content://media/images/1"),
            Uri.parse("content://media/images/2"),
        )
        val savePath = Uri.parse("content://tree/primary%3ADownload")
        coEvery { selectionRepository.putSelection(uris) } returns Result.success(Unit)
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.success(savePath)

        val outcome = createCoordinator().handlePendingShare(uris)

        assertEquals(ShareOutcome.NavigateToProcessing(savePath), outcome)
        assertNull(shareIntentHolder.uris.value)
        coVerify(exactly = 1) { selectionRepository.putSelection(uris) }
    }

    @Test
    fun `shared image without valid default save path navigates to save path selection`() = runTest {
        val uri = Uri.parse("content://media/images/1")
        coEvery { selectionRepository.putSelection(uri, false) } returns Result.success(Unit)
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.failure(
            IllegalStateException("no default save path"),
        )

        val outcome = createCoordinator().handlePendingShare(listOf(uri))

        assertEquals(ShareOutcome.NavigateToSavePathSelection, outcome)
        assertNull(shareIntentHolder.uris.value)
    }

    @Test
    fun `failed selection reports failure without checking save path`() = runTest {
        val uri = Uri.parse("content://media/images/1")
        coEvery { selectionRepository.putSelection(uri, false) } returns Result.failure(
            IllegalStateException("write failed"),
        )

        val outcome = createCoordinator().handlePendingShare(listOf(uri))

        assertEquals(ShareOutcome.SelectionFailed, outcome)
        assertNull(shareIntentHolder.uris.value)
        coVerify(exactly = 0) { settingsRepository.getPrivilegedDefaultSavePath() }
    }

    @Test
    fun `failed multi selection reports failure without checking save path`() = runTest {
        val uris = listOf(
            Uri.parse("content://media/images/1"),
            Uri.parse("content://media/images/2"),
        )
        coEvery { selectionRepository.putSelection(uris) } returns Result.failure(
            IllegalStateException("write failed"),
        )

        val outcome = createCoordinator().handlePendingShare(uris)

        assertEquals(ShareOutcome.SelectionFailed, outcome)
        assertNull(shareIntentHolder.uris.value)
        coVerify(exactly = 0) { settingsRepository.getPrivilegedDefaultSavePath() }
    }

    @Test
    fun `empty share reports unsupported without touching repositories`() = runTest {
        val outcome = createCoordinator().handlePendingShare(emptyList())

        assertEquals(ShareOutcome.Unsupported, outcome)
        assertNull(shareIntentHolder.uris.value)
        coVerify { selectionRepository wasNot Called }
        coVerify { settingsRepository wasNot Called }
    }
}
