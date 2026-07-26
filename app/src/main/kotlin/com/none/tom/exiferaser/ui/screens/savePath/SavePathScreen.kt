package com.none.tom.exiferaser.ui.screens.savePath

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.imageSavePathSelection.business.ImageSavePathSelectionSideEffect
import com.none.tom.exiferaser.imageSavePathSelection.business.ImageSavePathSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePathScreen(
    onNavigateToSelection: (Uri) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ImageSavePathSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    val chooseSavePathLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { result ->
        viewModel.handleSelection(result)
    }

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is ImageSavePathSelectionSideEffect.ChooseSavePath -> {
                    chooseSavePathLauncher.launch(sideEffect.openPath)
                }
                is ImageSavePathSelectionSideEffect.NavigateToSelection -> {
                    onNavigateToSelection(sideEffect.savePath)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.choose_save_path),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.choose_save_path_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.hasPrivilegedDefaultSavePath) {
                TextButton(
                    onClick = { viewModel.handleSelection(savePath = Uri.EMPTY) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.use_default_save_path))
                }
            }

            TextButton(
                onClick = { viewModel.chooseSelectionSavePath() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.choose_custom_save_path))
            }
        }
    }
}
