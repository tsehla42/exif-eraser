package com.tsehla.exiferaser.main.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tsehla.exiferaser.main.data.ImageSourcesRepository
import com.tsehla.exiferaser.main.data.MainRepository
import com.tsehla.exiferaser.main.data.SelectionRepository
import com.tsehla.exiferaser.settings.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class MainViewModelChooseSelectionNavigationRouteTest {

    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val mainRepository = mockk<MainRepository>()
    private val imageSourcesRepository = mockk<ImageSourcesRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MainViewModel(
        SavedStateHandle(),
        mainRepository,
        imageSourcesRepository,
        selectionRepository,
        settingsRepository,
    )

    @Test
    fun `default save path set navigates directly to processing without showing picker`() = runTest {
        val savePath = Uri.parse("content://tree/primary%3ADownload")
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.success(savePath)

        val viewModel = createViewModel()
        viewModel.container.sideEffectFlow.test {
            viewModel.chooseSelectionNavigationRoute()
            assertEquals(MainSideEffect.Navigate.ToSelection(savePath), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `default save path missing navigates to save path selection`() = runTest {
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.failure(
            IllegalStateException("no default save path")
        )

        val viewModel = createViewModel()
        viewModel.container.sideEffectFlow.test {
            viewModel.chooseSelectionNavigationRoute()
            assertEquals(MainSideEffect.Navigate.ToSelectionSavePath, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `camera capture with default save path navigates directly to processing`() = runTest {
        val savePath = Uri.parse("content://tree/primary%3ADCIM")
        coEvery { settingsRepository.getPrivilegedDefaultSavePath() } returns Result.success(savePath)

        val viewModel = createViewModel()
        viewModel.container.sideEffectFlow.test {
            viewModel.chooseSelectionNavigationRoute(isFromCamera = true)
            assertEquals(MainSideEffect.Navigate.ToSelection(savePath), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

    }
}
