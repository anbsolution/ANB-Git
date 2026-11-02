package com.anb.git.ui

import com.anb.git.data.LocalRepository
import com.anb.git.data.LocalRepositoryStorage
import com.anb.git.data.RepositoryDatabase
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Repositories("Repositories", Icons.Default.Folder),
    Editor("Editor", Icons.Default.Code),
    Settings("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnbGitApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = AppTab.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ANB Git", fontWeight = FontWeight.Bold)
                        Text(
                            "Offline Code & Version Control",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (tabs[selectedTab]) {
                AppTab.Home -> DashboardScreen(
                    onOpenRepositories = { selectedTab = 1 },
                    onOpenEditor = { selectedTab = 2 }
                )
                AppTab.Repositories -> RepositoriesScreen()
                AppTab.Editor -> EditorPlaceholderScreen()
                AppTab.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    onOpenRepositories: () -> Unit,
    onOpenEditor: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your workspace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Your code stays on this device. No account or internet connection required.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Text("Offline storage", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Private app storage is ready", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Repository files will be stored inside ANB Git's protected app directory.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Connection: Offline-ready", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Repositories", "0", Modifier.weight(1f))
                StatCard("Commits", "0", Modifier.weight(1f))
            }
        }
        item {
            Text("Quick actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenRepositories,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Repositories")
                }
                OutlinedButton(
                    onClick = onOpenEditor,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Editor")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Phase 1 foundation", fontWeight = FontWeight.Bold)
                    Text(
                        "Navigation, clean light theme and private storage bootstrap are included. Repository creation and Git operations arrive in later phases.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RepositoriesScreen() {
    val context = LocalContext.current
    val database = remember { RepositoryDatabase(context.applicationContext) }
    val repositories = remember {
        androidx.compose.runtime.mutableStateListOf<LocalRepository>()
    }
    var showCreate by remember { androidx.compose.runtime.mutableStateOf(false) }
    var editing by remember { androidx.compose.runtime.mutableStateOf<LocalRepository?>(null) }
    var deleting by remember { androidx.compose.runtime.mutableStateOf<LocalRepository?>(null) }
    var name by remember { androidx.compose.runtime.mutableStateOf("") }
    var error by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    fun refreshRepositories() {
        repositories.clear()
        repositories.addAll(database.getAll())
    }

    LaunchedEffect(Unit) {
        refreshRepositories()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Repositories",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${repositories.size} local repositories",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = {
                name = ""
                error = null
                showCreate = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(5.dp))
                Text("New")
            }
        }

        if (repositories.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "No repositories yet",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Create your first local project. It will be stored in ANB Git private storage.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = {
                        name = ""
                        error = null
                        showCreate = true
                    }) {
                        Text("Create repository")
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(repositories.size) { index ->
                    val repository = repositories[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(
                                modifier = Modifier.weight(1f).padding(start = 10.dp)
                            ) {
                                Text(repository.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Local • ${repository.folderName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.IconButton(onClick = {
                                editing = repository
                                name = repository.name
                                error = null
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                            androidx.compose.material3.IconButton(onClick = {
                                deleting = repository
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editing != null) {
        val current = editing
        AlertDialog(
            onDismissRequest = {
                showCreate = false
                editing = null
                error = null
            },
            title = {
                Text(if (current == null) "Create repository" else "Rename repository")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = null
                        },
                        label = { Text("Repository name") },
                        singleLine = true
                    )
                    Text(
                        "1–50 characters. Slash and backslash are not allowed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        if (current == null) {
                            val folder = LocalRepositoryStorage.create(context, name)
                            try {
                                database.insert(
                                    name.trim(),
                                    folder,
                                    System.currentTimeMillis()
                                )
                            } catch (exception: Exception) {
                                LocalRepositoryStorage.delete(context, folder)
                                throw exception
                            }
                        } else {
                            val newFolder = LocalRepositoryStorage.rename(
                                context,
                                current.folderName,
                                name
                            )
                            database.rename(current.id, name.trim(), newFolder)
                        }
                        refreshRepositories()
                        showCreate = false
                        editing = null
                        error = null
                    } catch (exception: Exception) {
                        error = exception.message ?: "Operation failed."
                    }
                }) {
                    Text(if (current == null) "Create" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreate = false
                    editing = null
                    error = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleting?.let { repository ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${repository.name}?") },
            text = {
                Text(
                    "This permanently deletes the repository folder and all files inside it. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        check(
                            LocalRepositoryStorage.delete(context, repository.folderName)
                        ) { "Could not delete repository folder." }
                        database.delete(repository.id)
                        refreshRepositories()
                        deleting = null
                    } catch (exception: Exception) {
                        deleting = null
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EditorPlaceholderScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(58.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Code Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "The offline code editor will be added in Phase 4.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", fontWeight = FontWeight.SemiBold)
                Text("Clean White + Royal Blue")
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storage", fontWeight = FontWeight.SemiBold)
                Text("Private app storage")
                Text("Package: com.anb.git", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About", fontWeight = FontWeight.SemiBold)
                Text("ANB Git v1.0 – Phase 1 foundation")
                Text("Designed to operate without internet access.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
