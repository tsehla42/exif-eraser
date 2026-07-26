package com.tsehla.exiferaser.settings.business

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tsehla.exiferaser.core.image.FileNameMode
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelDeleteConvertedTest {

    private val settingsRepository = mockk<SettingsRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(
        SavedStateHandle(),
        settingsRepository,
    )

    /**
     * Orbit intents run on a background worker, so state must be awaited
     * with real time instead of read synchronously.
     */
    private fun awaitState(
        viewModel: SettingsViewModel,
        predicate: (SettingsState) -> Boolean,
    ): SettingsState {
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            val state = viewModel.container.stateFlow.value
            if (predicate(state)) {
                return state
            }
            Thread.sleep(20)
        }
        return viewModel.container.stateFlow.value
    }

    private fun stubReadDefaultValues() {
        coEvery { settingsRepository.isRandomizeFileNamesEnabled() } returns false
        coEvery { settingsRepository.getDefaultOpenPathName() } returns ""
        coEvery { settingsRepository.getDefaultSavePathName() } returns ""
        coEvery { settingsRepository.isAutoDeleteEnabled() } returns false
        coEvery { settingsRepository.isDeleteConvertedEnabled() } returns false
        coEvery { settingsRepository.isPreserveOrientationEnabled() } returns true
        coEvery { settingsRepository.isShareByDefaultEnabled() } returns false
        coEvery { settingsRepository.getDefaultDisplayNameSuffix() } returns ""
        coEvery { settingsRepository.isLegacyImageSelectionEnabled() } returns false
        coEvery { settingsRepository.getDefaultNightModeName() } returns ""
        coEvery { settingsRepository.getThemeMode() } returns ThemeMode.SYSTEM
        coEvery { settingsRepository.getDynamicColor() } returns true
        coEvery { settingsRepository.isUseDateTimeNameEnabled() } returns false
        coEvery { settingsRepository.getFileNameMode() } returns FileNameMode.IMAGE_ID
    }

    @Test
    fun `readDefaultValues loads delete converted flag`() = runTest {
        stubReadDefaultValues()
        coEvery { settingsRepository.isDeleteConvertedEnabled() } returns true

        val viewModel = createViewModel()
        viewModel.readDefaultValues()

        val state = awaitState(viewModel) { it.isDeleteConvertedEnabled }
        assertTrue(state.isDeleteConvertedEnabled)
    }

    @Test
    fun `storeDeleteConverted persists setting and updates state`() = runTest {
        stubReadDefaultValues()
        coEvery { settingsRepository.putDeleteConverted(true) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.container.sideEffectFlow.test {
            viewModel.storeDeleteConverted(true)
            assertEquals(SettingsSideEffect.SettingResult.Success, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        val state = awaitState(viewModel) { it.isDeleteConvertedEnabled }
        assertTrue(state.isDeleteConvertedEnabled)
        coVerify(exactly = 1) { settingsRepository.putDeleteConverted(true) }
    }
}
