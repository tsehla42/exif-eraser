/*
 * Copyright (c) 2018-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.tsehla.exiferaser.settings.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tsehla.exiferaser.core.image.FileNameMode
import com.tsehla.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : ViewModel(),
    ContainerHost<SettingsState, SettingsSideEffect> {

    override val container = container<SettingsState, SettingsSideEffect>(
        initialState = SettingsState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            readDefaultValues()
        }
    )

    fun readDefaultValues() = intent {
        val isRandomizeFileNamesEnabled = settingsRepository.isRandomizeFileNamesEnabled()
        val defaultPathOpenName = settingsRepository.getDefaultOpenPathName()
        val defaultPathSaveName: String = settingsRepository.getDefaultSavePathName()
        val isAutoDeleteEnabled = settingsRepository.isAutoDeleteEnabled()
        val isDeleteConvertedEnabled = settingsRepository.isDeleteConvertedEnabled()
        val isPreserveOrientationEnabled = settingsRepository.isPreserveOrientationEnabled()
        val isShareByDefaultEnabled = settingsRepository.isShareByDefaultEnabled()
        val defaultDisplayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix()
        val isLegacyImageSelectionEnabled = settingsRepository.isLegacyImageSelectionEnabled()
        val defaultNightModeName = settingsRepository.getDefaultNightModeName()
        val themeMode = settingsRepository.getThemeMode()
        val useDynamicColor = settingsRepository.getDynamicColor()
        val isUseDateTimeNameEnabled = settingsRepository.isUseDateTimeNameEnabled()
        val fileNameMode = settingsRepository.getFileNameMode()
        reduce {
            state.copy(
                isRandomizeFileNamesEnabled = isRandomizeFileNamesEnabled,
                defaultOpenPathName = defaultPathOpenName,
                defaultSavePathName = defaultPathSaveName,
                isAutoDeleteEnabled = isAutoDeleteEnabled,
                isDeleteConvertedEnabled = isDeleteConvertedEnabled,
                isPreserveOrientationEnabled = isPreserveOrientationEnabled,
                isShareByDefaultEnabled = isShareByDefaultEnabled,
                defaultDisplayNameSuffix = defaultDisplayNameSuffix,
                isLegacyImageSelectionEnabled = isLegacyImageSelectionEnabled,
                defaultNightModeName = defaultNightModeName,
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                isUseDateTimeNameEnabled = isUseDateTimeNameEnabled,
                fileNameMode = fileNameMode,
            )
        }
    }

    fun storeRandomizeFileNames(isEnabled: Boolean) = storeSetting(
        update = { it.copy(isRandomizeFileNamesEnabled = isEnabled) },
        put = { settingsRepository.putRandomizeFileNames(isEnabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun handleDefaultPathOpen() = handleDefaultPath(
        getPath = settingsRepository::getPrivilegedDefaultOpenPath,
        onSuccess = { SettingsSideEffect.DefaultOpenPath.Select.Success(uri = it) },
        onFailure = SettingsSideEffect.DefaultOpenPath.Select.Failure,
    )

    fun clearDefaultPathOpen() = clearDefaultPath(
        put = { settingsRepository.putDefaultOpenPath(Uri.EMPTY, it) },
        getCurrentPath = settingsRepository::getPrivilegedDefaultOpenPath,
        onSuccess = SettingsSideEffect.DefaultOpenPath.Clear.Success,
        onFailure = SettingsSideEffect.DefaultOpenPath.Clear.Failure,
        update = { it.copy(defaultOpenPathName = "") },
    )

    fun storeDefaultPathOpen(uri: Uri) = storeDefaultPath(
        uri = uri,
        put = { new, current -> settingsRepository.putDefaultOpenPath(new, current) },
        getCurrentPath = settingsRepository::getPrivilegedDefaultOpenPath,
        getName = settingsRepository::getDefaultOpenPathName,
        onSuccess = SettingsSideEffect.DefaultOpenPath.Store.Success,
        onFailure = SettingsSideEffect.DefaultOpenPath.Store.Failure,
        update = { state, name -> state.copy(defaultOpenPathName = name) },
    )

    fun handleDefaultPathSave() = handleDefaultPath(
        getPath = settingsRepository::getPrivilegedDefaultSavePath,
        onSuccess = { SettingsSideEffect.DefaultSavePath.Select.Success(uri = it) },
        onFailure = SettingsSideEffect.DefaultSavePath.Select.Failure,
    )

    fun clearDefaultPathSave() = clearDefaultPath(
        put = { settingsRepository.putDefaultSavePath(Uri.EMPTY, it) },
        getCurrentPath = settingsRepository::getPrivilegedDefaultSavePath,
        onSuccess = SettingsSideEffect.DefaultSavePath.Clear.Success,
        onFailure = SettingsSideEffect.DefaultSavePath.Clear.Failure,
        update = { it.copy(defaultSavePathName = "") },
    )

    fun storeDefaultPathSave(uri: Uri) = storeDefaultPath(
        uri = uri,
        put = { new, current -> settingsRepository.putDefaultSavePath(new, current) },
        getCurrentPath = settingsRepository::getPrivilegedDefaultSavePath,
        getName = settingsRepository::getDefaultSavePathName,
        onSuccess = SettingsSideEffect.DefaultSavePath.Store.Success,
        onFailure = SettingsSideEffect.DefaultSavePath.Store.Failure,
        update = { state, name -> state.copy(defaultSavePathName = name) },
    )

    private fun handleDefaultPath(
        getPath: suspend () -> Result<Uri>,
        onSuccess: (Uri) -> SettingsSideEffect,
        onFailure: SettingsSideEffect,
    ) = intent {
        val result = getPath()
        if (result.isSuccess) {
            postSideEffect(onSuccess(result.getOrThrow()))
        } else {
            postSideEffect(onFailure)
        }
    }

    private fun clearDefaultPath(
        put: suspend (currentPath: Uri) -> Result<Unit>,
        getCurrentPath: suspend () -> Result<Uri>,
        onSuccess: SettingsSideEffect,
        onFailure: SettingsSideEffect,
        update: (SettingsState) -> SettingsState,
    ) = intent {
        val result = put(getCurrentPath().getOrDefault(Uri.EMPTY))
        if (result.isSuccess) {
            reduce { update(state) }
            postSideEffect(onSuccess)
        } else {
            postSideEffect(onFailure)
        }
    }

    private fun storeDefaultPath(
        uri: Uri,
        put: suspend (new: Uri, current: Uri) -> Result<Unit>,
        getCurrentPath: suspend () -> Result<Uri>,
        getName: suspend () -> String,
        onSuccess: SettingsSideEffect,
        onFailure: SettingsSideEffect,
        update: (SettingsState, String) -> SettingsState,
    ) = intent {
        val result = put(uri, getCurrentPath().getOrDefault(Uri.EMPTY))
        if (result.isSuccess) {
            val name = getName()
            reduce { update(state, name) }
            postSideEffect(onSuccess)
        } else {
            postSideEffect(onFailure)
        }
    }

    fun storeAutoDelete(isEnabled: Boolean) = storeSetting(
        update = { it.copy(isAutoDeleteEnabled = isEnabled) },
        put = { settingsRepository.putAutoDelete(isEnabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun storeDeleteConverted(isEnabled: Boolean) = storeSetting(
        update = { it.copy(isDeleteConvertedEnabled = isEnabled) },
        put = { settingsRepository.putDeleteConverted(isEnabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun storePreserveOrientation(isEnabled: Boolean) = storeSetting(
        update = { it.copy(isPreserveOrientationEnabled = isEnabled) },
        put = { settingsRepository.putPreserveOrientation(isEnabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun storeShareByDefault(isEnabled: Boolean) = storeSetting(
        update = { it.copy(isShareByDefaultEnabled = isEnabled) },
        put = { settingsRepository.putShareByDefault(isEnabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun handleDefaultDisplayNameSuffix() = intent {
        postSideEffect(
            SettingsSideEffect.Navigate.ToDefaultDisplayNameSuffix(
                defaultDisplayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix()
            )
        )
    }

    fun storeDefaultDisplayNameSuffix(value: String) = storeSetting(
        update = { it.copy(defaultDisplayNameSuffix = value) },
        put = { settingsRepository.putDefaultDisplayNameSuffix(value) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun storeLegacyImageSelection(isEnabled: Boolean) = storeSetting(
        update = { it.copy(isLegacyImageSelectionEnabled = isEnabled) },
        put = { settingsRepository.putSelectImagesLegacy(isEnabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun handleDefaultNightMode() = intent {
        postSideEffect(
            SettingsSideEffect.Navigate.ToDefaultNightMode(
                defaultNightMode = settingsRepository.getDefaultNightMode()
            )
        )
    }

    fun storeDefaultNightMode(value: Int) = intent {
        val result = settingsRepository.putDefaultNightMode(value)
        if (result.isSuccess) {
            val name = settingsRepository.getDefaultNightModeName()
            reduce {
                state.copy(defaultNightModeName = name)
            }
            postSideEffect(SettingsSideEffect.SettingResult.Success)
        } else {
            postSideEffect(SettingsSideEffect.SettingResult.Failure)
        }
    }

    fun storeThemeMode(mode: ThemeMode) = storeSetting(
        update = { it.copy(themeMode = mode) },
        put = { settingsRepository.putThemeMode(mode) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun storeUseDateTimeName(isEnabled: Boolean) = storeSetting(
        update = { it.copy(isUseDateTimeNameEnabled = isEnabled) },
        put = { settingsRepository.putUseDateTimeName(isEnabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun storeFileNameMode(mode: FileNameMode) = storeSetting(
        update = { it.copy(fileNameMode = mode) },
        put = { settingsRepository.putFileNameMode(mode) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    fun storeDynamicColor(enabled: Boolean) = storeSetting(
        update = { it.copy(useDynamicColor = enabled) },
        put = { settingsRepository.putDynamicColor(enabled) },
        onSuccess = SettingsSideEffect.SettingResult.Success,
        onFailure = SettingsSideEffect.SettingResult.Failure,
    )

    private fun storeSetting(
        update: (SettingsState) -> SettingsState,
        put: suspend () -> Result<Unit>,
        onSuccess: SettingsSideEffect,
        onFailure: SettingsSideEffect,
    ) = intent {
        val result = put()
        if (result.isSuccess) {
            reduce { update(state) }
            postSideEffect(onSuccess)
        } else {
            postSideEffect(onFailure)
        }
    }
}
