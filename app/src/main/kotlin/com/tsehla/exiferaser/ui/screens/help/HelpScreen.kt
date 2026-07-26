package com.tsehla.exiferaser.ui.screens.help

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tsehla.exiferaser.R
import com.tsehla.exiferaser.core.contract.ActivityResultContractViewUrl
import com.tsehla.exiferaser.ui.components.AppBottomBar
import com.tsehla.exiferaser.ui.components.ConfirmDialog

private const val REPOSITORY_URL = "https://github.com/tsehla42/exif-eraser"

private data class HelpItem(
    val icon: Painter,
    val title: String,
    val subtitle: String,
    val onClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToMain: () -> Unit,
) {
    val viewUrlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContractViewUrl(),
    ) { }
    var showRepositoryDialog by remember { mutableStateOf(false) }

    val items = listOf(
        HelpItem(
            icon = painterResource(R.drawable.baseline_code_24),
            title = stringResource(R.string.help_supported_formats),
            subtitle = stringResource(R.string.help_supported_formats_summary),
        ),

        HelpItem(
            icon = painterResource(R.drawable.baseline_git_24),
            title = stringResource(R.string.help_repository),
            subtitle = stringResource(R.string.help_repository_summary),
            onClick = { showRepositoryDialog = true },
        ),
        HelpItem(
            icon = painterResource(R.drawable.baseline_build_24),
            title = stringResource(R.string.help_version),
            subtitle = stringResource(R.string.help_version_summary),
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.help)) },
            )
        },
        bottomBar = {
            AppBottomBar(
                onHelp = { },
                onGrid = onNavigateToMain,
                onSettings = onNavigateToSettings,
                isHelpSelected = true,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(
                items = items,
                key = { it.title },
            ) { item ->
                HelpListItem(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = item.onClick,
                )
            }
        }
    }

    if (showRepositoryDialog) {
        ConfirmDialog(
            title = stringResource(R.string.help_repository),
            message = stringResource(R.string.help_repository_dialog_message),
            confirmButtonText = stringResource(R.string.help_repository_open),
            onConfirm = {
                showRepositoryDialog = false
                viewUrlLauncher.launch(Uri.parse(REPOSITORY_URL))
            },
            onDismiss = { showRepositoryDialog = false },
        )
    }
}

@Composable
private fun HelpListItem(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
