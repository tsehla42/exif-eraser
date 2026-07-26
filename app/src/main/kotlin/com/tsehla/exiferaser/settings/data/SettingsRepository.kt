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

package com.tsehla.exiferaser.settings.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import com.tsehla.exiferaser.core.di.DispatcherIo
import com.tsehla.exiferaser.core.extension.defaultNightModeDisplayValue
import com.tsehla.exiferaser.core.extension.defaultNightModes
import com.tsehla.exiferaser.core.extension.isNotEmpty
import com.tsehla.exiferaser.core.extension.isNotNullOrEmpty
import com.tsehla.exiferaser.core.extension.runCatchingResult
import com.tsehla.exiferaser.core.extension.suspendRunCatching
import com.tsehla.exiferaser.core.image.FileNameMode
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_AUTO_DELETE
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_DELETE_CONVERTED
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_DEFAULT_DISPLAY_NAME_SUFFIX
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_DEFAULT_NIGHT_MODE
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_DEFAULT_OPEN_PATH
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_DEFAULT_SAVE_PATH
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_LEGACY_IMAGE_SELECTION
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_PRESERVE_ORIENTATION
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_RANDOMIZE_FILE_NAMES
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_SHARE_BY_DEFAULT
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_THEME_MODE
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_DYNAMIC_COLOR
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_FILE_NAME_MODE
import com.tsehla.exiferaser.core.util.SETTINGS_KEY_USE_DATETIME_NAME
import com.tsehla.exiferaser.core.util.defaultNightMode
import com.tsehla.exiferaser.settings.business.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @param:DispatcherIo private val dispatcherIo: CoroutineDispatcher,
) {

    private val keyRandomizeFileNames = booleanPreferencesKey(SETTINGS_KEY_RANDOMIZE_FILE_NAMES)
    private val keyDefaultOpenPath = stringPreferencesKey(SETTINGS_KEY_DEFAULT_OPEN_PATH)
    private val keyDefaultSavePath = stringPreferencesKey(SETTINGS_KEY_DEFAULT_SAVE_PATH)
    private val keyAutoDelete = booleanPreferencesKey(SETTINGS_KEY_AUTO_DELETE)
    private val keyDeleteConverted = booleanPreferencesKey(SETTINGS_KEY_DELETE_CONVERTED)
    private val keyPreserveOrientation = booleanPreferencesKey(SETTINGS_KEY_PRESERVE_ORIENTATION)
    private val keyShareByDefault = booleanPreferencesKey(SETTINGS_KEY_SHARE_BY_DEFAULT)
    private val keyDefaultDisplayNameSuffix =
        stringPreferencesKey(SETTINGS_KEY_DEFAULT_DISPLAY_NAME_SUFFIX)
    private val keyLegacyImageSelection = booleanPreferencesKey(SETTINGS_KEY_LEGACY_IMAGE_SELECTION)
    private val keyDefaultNightMode = intPreferencesKey(SETTINGS_KEY_DEFAULT_NIGHT_MODE)
    private val keyThemeMode = intPreferencesKey(SETTINGS_KEY_THEME_MODE)
    private val keyDynamicColor = booleanPreferencesKey(SETTINGS_KEY_DYNAMIC_COLOR)
    private val keyUseDateTimeName = booleanPreferencesKey(SETTINGS_KEY_USE_DATETIME_NAME)
    private val keyFileNameMode = intPreferencesKey(SETTINGS_KEY_FILE_NAME_MODE)

    suspend fun putDefaultOpenPath(
        newDefaultOpenPath: Uri,
        currentDefaultOpenPath: Uri
    ): Result<Unit> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            if (newDefaultOpenPath.isNotEmpty()) {
                if (takePersistablePermissions(
                        contentResolver = context.contentResolver,
                        uri = newDefaultOpenPath
                    ).isFailure
                ) {
                    error("takePersistablePermissions() failed")
                }
            }
            if (currentDefaultOpenPath.isNotEmpty()) {
                releasePersistablePermissions(context.contentResolver, currentDefaultOpenPath)
            }
            dataStore.edit { preferences ->
                preferences[keyDefaultOpenPath] = newDefaultOpenPath.toString()
            }
            Result.success(Unit)
        }.getOrElse { exception ->
            Timber.e(exception)
            if (newDefaultOpenPath.isNotEmpty()) {
                releasePersistablePermissions(context.contentResolver, newDefaultOpenPath)
            }
            Result.failure(exception)
        }
    }

    private suspend fun getDefaultOpenPath(): Uri = dataStore
        .data
        .catch { exception -> Timber.e(exception) }
        .map { preferences -> preferences[keyDefaultOpenPath]?.toUri() }
        .firstOrNull()
        ?: Uri.EMPTY

    suspend fun getDefaultOpenPathName(): String = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            val defaultOpenPath = getDefaultOpenPath()
            if (!defaultOpenPath.isNotNullOrEmpty()) {
                error("defaultOpenPath is null or empty")
            }
            DocumentFile.fromTreeUri(context, defaultOpenPath)?.name.orEmpty()
        }.getOrElse { exception ->
            Timber.e(exception)
            ""
        }
    }

    suspend fun getPrivilegedDefaultOpenPath(): Result<Uri> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            val defaultOpenPath = getDefaultOpenPath()
            if (!defaultOpenPath.isNotNullOrEmpty()) {
                error("defaultOpenPath is null or empty")
            }
            val exists = DocumentFile.fromTreeUri(context, defaultOpenPath)?.exists() == true
            if (!exists) {
                releasePersistablePermissions(context.contentResolver, defaultOpenPath)
                error("defaultOpenPath does not exist anymore")
            }
            if (!hasPersistablePermissions(context.contentResolver, defaultOpenPath)) {
                error("defaultOpenPath is missing persistable permissions")
            }
            Result.success(defaultOpenPath)
        }.getOrElse { exception ->
            Timber.e(exception)
            Result.failure(exception)
        }
    }

    suspend fun putDefaultSavePath(
        newDefaultSavePath: Uri,
        currentDefaultSavePath: Uri
    ): Result<Unit> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            if (newDefaultSavePath.isNotEmpty()) {
                val result = takePersistablePermissions(
                    contentResolver = context.contentResolver,
                    uri = newDefaultSavePath,
                    write = true
                )
                if (result.isFailure) {
                    error("takePersistablePermissions() failed")
                }
            }
            if (currentDefaultSavePath.isNotEmpty()) {
                releasePersistablePermissions(
                    contentResolver = context.contentResolver,
                    uri = currentDefaultSavePath,
                    write = true
                )
            }
            dataStore.edit { preferences ->
                preferences[keyDefaultSavePath] = newDefaultSavePath.toString()
            }
            Result.success(Unit)
        }.getOrElse { exception ->
            Timber.e(exception)
            if (newDefaultSavePath.isNotEmpty()) {
                releasePersistablePermissions(
                    contentResolver = context.contentResolver,
                    uri = newDefaultSavePath,
                    write = true
                )
            }
            Result.failure(exception)
        }
    }

    private suspend fun getDefaultSavePath(): Uri = dataStore
        .data
        .catch { exception -> Timber.e(exception) }
        .map { preferences -> preferences[keyDefaultSavePath]?.toUri() }
        .firstOrNull()
        ?: Uri.EMPTY

    suspend fun getDefaultSavePathName(): String = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            val defaultSavePath = getDefaultSavePath()
            if (!defaultSavePath.isNotNullOrEmpty()) {
                error("defaultSavePath is null or empty")
            }
            val pathSegments = defaultSavePath.pathSegments
            val relevantSegments = pathSegments
                .filter { it != "tree" }
                .map { segment -> segment.substringAfter(":", segment) }
                .takeLast(2)
            relevantSegments.joinToString("/")
        }.getOrElse { exception ->
            Timber.e(exception)
            ""
        }
    }

    private suspend fun readBooleanPref(
        key: Preferences.Key<Boolean>,
        default: Boolean = false,
    ): Boolean = dataStore
        .data
        .catch { exception -> Timber.e(exception) }
        .map { preferences -> preferences[key] ?: default }
        .firstOrNull() == true

    private suspend fun writeBooleanPref(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ): Result<Unit> = dispatcherIo.runCatchingResult {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun isRandomizeFileNamesEnabled(): Boolean = readBooleanPref(keyRandomizeFileNames)

    suspend fun putRandomizeFileNames(value: Boolean): Result<Unit> =
        writeBooleanPref(keyRandomizeFileNames, value)

    suspend fun isLegacyImageSelectionEnabled(): Boolean =
        readBooleanPref(keyLegacyImageSelection, default = Build.VERSION.SDK_INT < 33)

    suspend fun putSelectImagesLegacy(value: Boolean): Result<Unit> =
        writeBooleanPref(keyLegacyImageSelection, value)

    suspend fun getDefaultSavePathForProcessing(): Uri = withContext(dispatcherIo) {
        val defaultSavePath = getDefaultSavePath()
        if (defaultSavePath.isNotNullOrEmpty()) {
            defaultSavePath
        } else {
            Uri.EMPTY
        }
    }

    suspend fun getPrivilegedDefaultSavePath(): Result<Uri> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            val defaultSavePath = getDefaultSavePath()
            if (!defaultSavePath.isNotNullOrEmpty()) {
                error("defaultSavePath is null or empty")
            }
            val exists = DocumentFile.fromTreeUri(context, defaultSavePath)?.exists() == true
            if (!exists) {
                releasePersistablePermissions(
                    contentResolver = context.contentResolver,
                    uri = defaultSavePath,
                    write = true
                )
                error("defaultSavePath does not exist anymore")
            }
            if (!hasPersistablePermissions(
                    resolver = context.contentResolver,
                    uri = defaultSavePath,
                    write = true
                )
            ) {
                error("defaultSavePath is missing persistable permissions")
            }
            Result.success(defaultSavePath)
        }.getOrElse { exception ->
            Timber.e(exception)
            Result.failure(exception)
        }
    }

    suspend fun putAutoDelete(value: Boolean): Result<Unit> =
        writeBooleanPref(keyAutoDelete, value)

    suspend fun isAutoDeleteEnabled(): Boolean = readBooleanPref(keyAutoDelete)

    suspend fun putDeleteConverted(value: Boolean): Result<Unit> =
        writeBooleanPref(keyDeleteConverted, value)

    suspend fun isDeleteConvertedEnabled(): Boolean = readBooleanPref(keyDeleteConverted)

    suspend fun putPreserveOrientation(value: Boolean): Result<Unit> =
        writeBooleanPref(keyPreserveOrientation, value)

    suspend fun isPreserveOrientationEnabled(): Boolean = readBooleanPref(keyPreserveOrientation)

    suspend fun putShareByDefault(value: Boolean): Result<Unit> =
        writeBooleanPref(keyShareByDefault, value)

    suspend fun isShareByDefaultEnabled(): Boolean = readBooleanPref(keyShareByDefault)

    suspend fun putDefaultDisplayNameSuffix(value: String): Result<Unit> =
        dispatcherIo.runCatchingResult {
            dataStore.edit { preferences -> preferences[keyDefaultDisplayNameSuffix] = value }
        }

    suspend fun getDefaultDisplayNameSuffix(): String = dataStore
        .data
        .catch { exception -> Timber.e(exception) }
        .map { preferences -> preferences[keyDefaultDisplayNameSuffix] }
        .firstOrNull()
        .orEmpty()

    suspend fun getDefaultNightMode(): Int = dataStore
        .data
        .map { preferences -> preferences[keyDefaultNightMode] }
        .firstOrNull()
        ?: defaultNightMode

    suspend fun getDefaultNightModeName(): String = withContext(dispatcherIo) {
        context
            .defaultNightModes()
            .getOrDefault(getDefaultNightMode(), context.defaultNightModeDisplayValue())
    }

    suspend fun putDefaultNightMode(value: Int): Result<Unit> = dispatcherIo.runCatchingResult {
        dataStore.edit { preferences -> preferences[keyDefaultNightMode] = value }
    }

    suspend fun getThemeMode(): ThemeMode = dataStore
        .data
        .catch { exception -> Timber.e(exception) }
        .map { preferences -> ThemeMode.entries.getOrElse(preferences[keyThemeMode] ?: 3) { ThemeMode.SYSTEM } }
        .firstOrNull()
        ?: ThemeMode.SYSTEM

    suspend fun putThemeMode(value: ThemeMode): Result<Unit> = dispatcherIo.runCatchingResult {
        dataStore.edit { preferences -> preferences[keyThemeMode] = value.ordinal }
    }

    suspend fun getDynamicColor(): Boolean = dataStore
        .data
        .catch { exception -> Timber.e(exception) }
        .map { preferences -> preferences[keyDynamicColor] ?: true }
        .firstOrNull()
        ?: true

    suspend fun putDynamicColor(value: Boolean): Result<Unit> =
        writeBooleanPref(keyDynamicColor, value)

    suspend fun isUseDateTimeNameEnabled(): Boolean = readBooleanPref(keyUseDateTimeName)

    suspend fun putUseDateTimeName(value: Boolean): Result<Unit> =
        writeBooleanPref(keyUseDateTimeName, value)

    suspend fun getFileNameMode(): FileNameMode = dataStore
        .data
        .catch { exception -> Timber.e(exception) }
        .map { preferences ->
            val stored = preferences[keyFileNameMode]
            if (stored != null) {
                FileNameMode.entries.getOrElse(stored) { FileNameMode.IMAGE_ID }
            } else {
                val randomize = preferences[booleanPreferencesKey(SETTINGS_KEY_RANDOMIZE_FILE_NAMES)] ?: false
                val useDatetime = preferences[booleanPreferencesKey(SETTINGS_KEY_USE_DATETIME_NAME)] ?: false
                when {
                    randomize -> FileNameMode.RANDOM
                    useDatetime -> FileNameMode.CONVERSION_DATE
                    else -> FileNameMode.IMAGE_ID
                }
            }
        }
        .firstOrNull()
        ?: FileNameMode.IMAGE_ID

    suspend fun putFileNameMode(value: FileNameMode): Result<Unit> = dispatcherIo.runCatchingResult {
        dataStore.edit { preferences ->
            preferences[keyFileNameMode] = value.ordinal
        }
    }

    private suspend fun hasPersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false
    ): Boolean {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                resolver.persistedUriPermissions.any { permission ->
                    if (permission.uri != uri) {
                        false
                    } else {
                        if (read && !permission.isReadPermission) {
                            return@any false
                        }
                        if (write && !permission.isWritePermission) {
                            return@any false
                        }
                        true
                    }
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    private suspend fun takePersistablePermissions(
        contentResolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false,
    ): Result<Unit> = dispatcherIo.runCatchingResult {
        var flags = 0
        if (read) {
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (write) {
            flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        contentResolver.takePersistableUriPermission(uri, flags)
    }

    private suspend fun releasePersistablePermissions(
        contentResolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false,
    ): Result<Unit> = dispatcherIo.runCatchingResult {
        var flags = 0
        if (read) {
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (write) {
            flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        contentResolver.releasePersistableUriPermission(uri, flags)
    }
}
