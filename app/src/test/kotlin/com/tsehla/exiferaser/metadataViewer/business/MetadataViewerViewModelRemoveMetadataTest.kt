package com.tsehla.exiferaser.metadataViewer.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tsehla.exiferaser.main.data.SelectionRepository
import com.tsehla.exiferaser.metadataViewer.data.MetadataViewerRepository
import com.tsehla.exiferaser.settings.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MetadataViewerViewModelRemoveMetadataTest {

    private val repository = mockk<MetadataViewerRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MetadataViewerViewModel(
        SavedStateHandle(),
        repository,
        selectionRepository,
        settingsRepository,
    )

    @Test
    fun `default save path set navigates directly to processing`() = runTest {
        val imageUri = Uri.parse("content://media/images/1")
        val savePath = Uri.parse("content://tree/primary%3ADownload")
        coEvery { selectionRepository.putSelection(imageUri) } returns Result.success(Unit)
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.success(savePath)

        val viewModel = createViewModel()
        viewModel.container.sideEffectFlow.test {
            viewModel.removeMetadata(imageUri)
            assertEquals(MetadataViewerSideEffect.NavigateToProcessing(savePath), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `default save path missing navigates to save path selection`() = runTest {
        val imageUri = Uri.parse("content://media/images/1")
        coEvery { selectionRepository.putSelection(imageUri) } returns Result.success(Unit)
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.failure(
            IllegalStateException("no default save path")
        )

        val viewModel = createViewModel()
        viewModel.container.sideEffectFlow.test {
            viewModel.removeMetadata(imageUri)
            assertEquals(MetadataViewerSideEffect.NavigateToSavePathSelection, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
