package com.tsehla.exiferaser.ui.screens.savePath

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.imageSavePathSelection.business.ImageSavePathSelectionSideEffect
import com.tsehla.exiferaser.imageSavePathSelection.business.ImageSavePathSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePathScreen(
    onNavigateToSelection: (Uri) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ImageSavePathSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

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
                            painter = painterResource(R.drawable.baseline_arrow_back_24),
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
                    OutlinedButton(
                        onClick = { viewModel.handleSelection(savePath = Uri.EMPTY) },
                        modifier = Modifier.fillMaxWidth(0.8f),
                    ) {
                        Text(text = stringResource(R.string.use_default_save_path))
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
