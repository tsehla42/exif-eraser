package com.tsehla.exiferaser.main.data

import android.net.Uri
import com.tsehla.exiferaser.settings.data.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/**
 * Decides how an incoming share is processed, independently of which screen is
 * currently visible. Pending URIs are handed in from the activity level so a share
 * received while the user is on the help or settings tab still opens the
 * conversion flow immediately.
 */
@Singleton
class ShareIntentCoordinator @Inject constructor(
    private val shareIntentHolder: ShareIntentHolder,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository,
) {

    val pendingShare: StateFlow<List<Uri>?> = shareIntentHolder.uris

    suspend fun handlePendingShare(uris: List<Uri>): ShareOutcome {
        shareIntentHolder.consume()
        if (uris.isEmpty()) {
            return ShareOutcome.Unsupported
        }
        val selectionResult = if (uris.size < 2) {
            selectionRepository.putSelection(uris.first())
        } else {
            selectionRepository.putSelection(uris)
        }
        if (selectionResult.isFailure) {
            return ShareOutcome.SelectionFailed
        }
        val savePathResult = settingsRepository.getPrivilegedDefaultSavePath()
        return savePathResult.fold(
            onSuccess = { savePath -> ShareOutcome.NavigateToProcessing(savePath) },
            onFailure = { ShareOutcome.NavigateToSavePathSelection },
        )
    }
}

sealed interface ShareOutcome {
    data object Unsupported : ShareOutcome

    data object SelectionFailed : ShareOutcome

    data class NavigateToProcessing(val savePath: Uri) : ShareOutcome

    data object NavigateToSavePathSelection : ShareOutcome
}
