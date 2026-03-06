package com.example.redditshelf.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.redditshelf.data.model.ItemAnnotation
import com.example.redditshelf.data.model.ItemStatus
import com.example.redditshelf.data.model.SavedItem
import com.example.redditshelf.data.model.ShelfFolder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenAuth: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<SavedItem?>(null) }

    LaunchedEffect(state.error, state.message) {
        val message = state.error ?: state.message
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearTransientMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reddit Shelf") },
                actions = {
                    IconButton(onClick = { viewModel.refreshShelf() }, enabled = state.token != null && !state.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.signOut() }, enabled = state.token != null) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConfigCard(
                    state = state,
                    onSaveConfig = viewModel::saveConfig,
                    onAuthorize = {
                        val url = viewModel.buildAuthorizeUrl()
                        if (url != null) onOpenAuth(url)
                    }
                )
            }

            if (state.profile != null) {
                item {
                    ProfileSummary(
                        username = state.profile!!.name,
                        karma = state.profile!!.totalKarma,
                        folderCount = state.shelfData.folders.size,
                        onAddFolder = { showAddFolderDialog = true }
                    )
                }

                item {
                    FolderStrip(folders = state.shelfData.folders)
                }

                if (state.items.isEmpty() && state.isLoading) {
                    item { CenterLoading("Loading saved items…") }
                } else {
                    items(state.items, key = { it.name }) { item ->
                        val annotation = state.shelfData.annotations.firstOrNull { it.thingId == item.thingId }
                        SavedItemCard(
                            item = item,
                            annotation = annotation,
                            folders = state.shelfData.folders,
                            onOrganize = { selectedItem = item }
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            OutlinedButton(onClick = { viewModel.loadMore() }, enabled = !state.isPaging) {
                                if (state.isPaging) {
                                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Load more")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFolderDialog) {
        AddFolderDialog(
            onDismiss = { showAddFolderDialog = false },
            onConfirm = {
                viewModel.addFolder(it)
                showAddFolderDialog = false
            }
        )
    }

    selectedItem?.let { item ->
        val annotation = state.shelfData.annotations.firstOrNull { it.thingId == item.thingId }
        AnnotationDialog(
            item = item,
            annotation = annotation,
            folders = state.shelfData.folders,
            onDismiss = { selectedItem = null },
            onSave = { folderIds, tags, note, status ->
                viewModel.saveAnnotation(item, folderIds, tags, note, status)
                selectedItem = null
            }
        )
    }
}

@Composable
private fun ConfigCard(
    state: MainUiState,
    onSaveConfig: (String, String, String) -> Unit,
    onAuthorize: () -> Unit
) {
    var clientId by remember(state.config.clientId) { mutableStateOf(state.config.clientId) }
    var redirectUri by remember(state.config.redirectUri) { mutableStateOf(state.config.redirectUri) }
    var userAgent by remember(state.config.userAgent) { mutableStateOf(state.config.userAgent) }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("OAuth configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "This app is built as a Reddit installed app. Enter your approved client ID and keep the redirect URI matched to the one registered in Reddit.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(value = clientId, onValueChange = { clientId = it }, label = { Text("Client ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = redirectUri, onValueChange = { redirectUri = it }, label = { Text("Redirect URI") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = userAgent, onValueChange = { userAgent = it }, label = { Text("User-Agent") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSaveConfig(clientId, redirectUri, userAgent) }) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save config")
                }
                OutlinedButton(onClick = onAuthorize, enabled = !state.isLoading) {
                    Text(if (state.token == null) "Connect Reddit" else "Reconnect")
                }
            }
        }
    }
}

@Composable
private fun ProfileSummary(username: String, karma: Int, folderCount: Int, onAddFolder: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Signed in as u/$username", fontWeight = FontWeight.Bold)
                Text("Total karma: $karma")
                Text("Folders: $folderCount")
            }
            OutlinedButton(onClick = onAddFolder) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Folder")
            }
        }
    }
}

@Composable
private fun FolderStrip(folders: List<ShelfFolder>) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Personal folders", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                folders.forEach { folder ->
                    AssistChip(onClick = { }, label = { Text(folder.name) })
                }
            }
        }
    }
}

@Composable
private fun SavedItemCard(
    item: SavedItem,
    annotation: ItemAnnotation?,
    folders: List<ShelfFolder>,
    onOrganize: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("r/${item.subreddit} • u/${item.author} • ${item.type}")
            if (item.subtitle.isNotBlank()) {
                Text(item.subtitle.take(240))
            }
            if (annotation != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    annotation.folderIds.mapNotNull { id -> folders.firstOrNull { it.id == id } }.forEach {
                        AssistChip(onClick = { }, label = { Text(it.name) })
                    }
                    annotation.tags.forEach {
                        AssistChip(onClick = { }, label = { Text("#$it") })
                    }
                    AssistChip(onClick = { }, label = { Text(annotation.status.name) })
                }
                if (annotation.note.isNotBlank()) Text("Note: ${annotation.note}")
            }
            OutlinedButton(onClick = onOrganize) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Organize")
            }
        }
    }
}

@Composable
private fun AddFolderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { if (folderName.isNotBlank()) onConfirm(folderName) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder name") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun AnnotationDialog(
    item: SavedItem,
    annotation: ItemAnnotation?,
    folders: List<ShelfFolder>,
    onDismiss: () -> Unit,
    onSave: (Set<String>, Set<String>, String, ItemStatus) -> Unit
) {
    var selectedFolderIds by remember { mutableStateOf(annotation?.folderIds ?: emptySet()) }
    var tagsText by remember { mutableStateOf(annotation?.tags?.joinToString(", ") ?: "") }
    var note by remember { mutableStateOf(annotation?.note ?: "") }
    var status by remember { mutableStateOf(annotation?.status ?: ItemStatus.INBOX) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                onSave(selectedFolderIds, tags, note.trim(), status)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Organize: ${item.title.take(36)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Folders")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    folders.forEach { folder ->
                        val selected = folder.id in selectedFolderIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedFolderIds = if (selected) selectedFolderIds - folder.id else selectedFolderIds + folder.id
                            },
                            label = { Text(folder.name) }
                        )
                    }
                }
                OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Private note") }, modifier = Modifier.fillMaxWidth())
                Text("Status")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemStatus.entries.forEach { candidate ->
                        FilterChip(
                            selected = status == candidate,
                            onClick = { status = candidate },
                            label = { Text(candidate.name) }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CenterLoading(label: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(label)
    }
}
