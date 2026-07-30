package com.none.tom.exiferaser.ui.screens.savePath

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    var skipSavePathSelection by remember { mutableStateOf(false) }

    val pickDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { viewModel.handleSelection(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is ImageSavePathSelectionSideEffect.ChooseSavePath -> {
                    pickDocumentTreeLauncher.launch(sideEffect.openPath)
                }
                is ImageSavePathSelectionSideEffect.NavigateToSelection -> {
                    onNavigateToSelection(sideEffect.savePath)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_save_path)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            Text(
                text = stringResource(R.string.choose_save_path_description),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.hasPrivilegedDefaultSavePath) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(bottom = 0.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setSkipSavePathSelection(skipSavePathSelection)
                                viewModel.handleSelection(savePath = Uri.EMPTY)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(R.string.use_default_save_path))
                        }
                        Checkbox(
                            checked = skipSavePathSelection,
                            onCheckedChange = { skipSavePathSelection = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (skipSavePathSelection) {
                            Text(
                                text = stringResource(R.string.settings_skip_save_path_selection),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = { viewModel.chooseSelectionSavePath() },
                    modifier = Modifier.fillMaxWidth(0.8f),
                ) {
                    Text(text = stringResource(R.string.choose_custom_save_path))
                }
            }
        }
    }
}
