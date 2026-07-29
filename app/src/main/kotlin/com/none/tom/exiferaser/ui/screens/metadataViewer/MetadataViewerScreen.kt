package com.none.tom.exiferaser.ui.screens.metadataViewer

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.metadataViewer.business.MetadataViewerViewModel
import com.none.tom.exiferaser.ui.components.EmptyState
import com.none.tom.exiferaser.ui.components.ErrorState
import com.none.tom.exiferaser.ui.components.LoadingState
import com.none.tom.exiferaser.ui.components.MetadataGroupHeader
import com.none.tom.exiferaser.ui.components.MetadataTagItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataViewerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRemoveMetadata: (Uri) -> Unit,
    uri: Uri,
    displayName: String,
    hideDeleteButton: Boolean = false,
    viewModel: MetadataViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(uri, displayName) {
        viewModel.loadMetadata(uri = uri, displayName = displayName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName.ifEmpty {
                            stringResource(R.string.metadata_viewer_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.metadataGroups.isNotEmpty() && !state.isLoading && !hideDeleteButton) {
                FloatingActionButton(
                    onClick = { onRemoveMetadata(uri) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.remove_metadata),
                    )
                }
            }
        },
        bottomBar = {
            Column {
                if (state.metadataGroups.isNotEmpty() && !state.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (state.isRawMode) viewModel.toggleDisplayMode()
                                }
                                .padding(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ViewList,
                                contentDescription = stringResource(R.string.metadata_toggle_pretty),
                                tint = if (!state.isRawMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.pretty),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!state.isRawMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (!state.isRawMode) viewModel.toggleDisplayMode()
                                }
                                .padding(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = stringResource(R.string.metadata_toggle_raw),
                                tint = if (state.isRawMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.raw),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isRawMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!state.isRawMode) {
                            val allExpanded = state.expandedGroups.size == state.metadataGroups.size
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (allExpanded) viewModel.collapseAll() else viewModel.expandAll()
                                    }
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    imageVector = if (allExpanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                                    contentDescription = stringResource(
                                        if (allExpanded) R.string.metadata_collapse_all else R.string.metadata_expand_all
                                    ),
                                )
                                Text(
                                    text = stringResource(if (allExpanded) R.string.collapse else R.string.expand),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                NavigationBar(
                    containerColor = if (MaterialTheme.colorScheme.background == Color.Black) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = stringResource(R.string.help),
                            )
                        },
                        label = { Text(stringResource(R.string.help_short)) },
                        selected = false,
                        onClick = onNavigateToHelp,
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = stringResource(R.string.paste_images),
                            )
                        },
                        label = { Text(stringResource(R.string.paste_short)) },
                        selected = false,
                        onClick = { /* paste */ },
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.GridOn,
                                contentDescription = stringResource(R.string.image_sources),
                            )
                        },
                        label = { Text(stringResource(R.string.grid_short)) },
                        selected = false,
                        onClick = onNavigateToMain,
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings),
                            )
                        },
                        label = { Text(stringResource(R.string.settings_short)) },
                        selected = false,
                        onClick = onNavigateToSettings,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }

                state.error != null -> {
                    ErrorState(
                        message = state.error ?: stringResource(R.string.metadata_error),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                state.metadataGroups.isEmpty() -> {
                    EmptyState(
                        title = stringResource(R.string.metadata_no_data),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                state.isRawMode -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        state.metadataGroups.forEach { group ->
                            item(key = "header_${group.name}") {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            group.tags.forEach { tag ->
                                item(key = "${group.name}_${tag.tagName}") {
                                    Text(
                                        text = "${tag.tagName}: ${tag.description}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.metadataGroups,
                            key = { it.name },
                        ) { group ->
                            Column {
                                MetadataGroupHeader(
                                    groupName = group.name,
                                    tagCount = group.tags.size,
                                    isExpanded = group.name in state.expandedGroups,
                                    onToggle = { viewModel.toggleGroup(group.name) },
                                )
                                AnimatedVisibility(visible = group.name in state.expandedGroups) {
                                    Column {
                                        group.tags.forEach { tag ->
                                            MetadataTagItem(tag = tag)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
