package com.tsehla.exiferaser.metadataViewer.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tsehla.exiferaser.main.data.SelectionRepository
import com.tsehla.exiferaser.metadataViewer.data.MetadataViewerRepository
import com.tsehla.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class MetadataViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MetadataViewerRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel(),
    ContainerHost<MetadataViewerState, MetadataViewerSideEffect> {

    override val container = container<MetadataViewerState, MetadataViewerSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = MetadataViewerState(),
    )

    fun loadMetadata(uri: Uri, displayName: String) = intent {
        reduce { state.copy(isLoading = true, imageUri = uri, displayName = displayName) }
        val result = repository.readMetadata(uri)
        reduce {
            state.copy(
                isLoading = false,
                metadataGroups = result.getOrElse { emptyList() },
                error = result.exceptionOrNull()?.message,
            )
        }
        result.exceptionOrNull()?.let { e ->
            postSideEffect(MetadataViewerSideEffect.Error(e.message ?: "Unknown error"))
        }
    }

    fun toggleGroup(groupName: String) = intent {
        reduce {
            state.copy(
                expandedGroups = if (groupName in state.expandedGroups) {
                    state.expandedGroups - groupName
                } else {
                    state.expandedGroups + groupName
                },
            )
        }
    }

    fun expandAll() = intent {
        reduce {
            state.copy(expandedGroups = state.metadataGroups.map { it.name }.toSet())
        }
    }

    fun collapseAll() = intent {
        reduce { state.copy(expandedGroups = emptySet()) }
    }

    fun toggleDisplayMode() = intent {
        val newRawMode = !state.isRawMode
        if (newRawMode) {
            reduce { state.copy(isRawMode = true, expandedGroups = emptySet()) }
        } else {
            val allGroups = state.metadataGroups.map { it.name }.toSet()
            reduce { state.copy(isRawMode = false, expandedGroups = allGroups) }
        }
    }

    fun removeMetadata(uri: Uri) = intent {
        val putResult = selectionRepository.putSelection(uri)
        if (putResult.isFailure) {
            postSideEffect(MetadataViewerSideEffect.Error("Failed to prepare image for processing"))
            return@intent
        }
        val result = settingsRepository.getPrivilegedDefaultSavePath()
        if (result.isSuccess) {
            postSideEffect(MetadataViewerSideEffect.NavigateToProcessing(result.getOrThrow()))
        } else {
            postSideEffect(MetadataViewerSideEffect.NavigateToSavePathSelection)
        }
    }
}
