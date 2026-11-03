package com.anb.git.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField

import com.anb.git.data.LocalRepository
import com.anb.git.data.LocalRepositoryStorage
import com.anb.git.data.RepositoryDatabase
import com.anb.git.data.GitRepositoryManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
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
                AppTab.Editor -> CodeEditorScreen()
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
    val repositories = remember { androidx.compose.runtime.mutableStateListOf<LocalRepository>() }
    var selected by remember { androidx.compose.runtime.mutableStateOf<LocalRepository?>(null) }
    var showCreate by remember { androidx.compose.runtime.mutableStateOf(false) }
    var editing by remember { androidx.compose.runtime.mutableStateOf<LocalRepository?>(null) }
    var deleting by remember { androidx.compose.runtime.mutableStateOf<LocalRepository?>(null) }
    var name by remember { androidx.compose.runtime.mutableStateOf("") }
    var error by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    fun refresh() {
        repositories.clear()
        repositories.addAll(database.getAll())
        if (selected != null) selected = repositories.firstOrNull { it.id == selected!!.id }
    }
    LaunchedEffect(Unit) { refresh() }

    val current = selected
    if (current != null) {
        GitRepositoryScreen(
            repository = current,
            onBack = { selected = null }
        )
    } else {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Repositories", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${repositories.size} local repositories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { name = ""; error = null; showCreate = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(5.dp)); Text("New")
                }
            }
            if (repositories.isEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("No repositories yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Create your first local project.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { name = ""; error = null; showCreate = true }) { Text("Create repository") }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(repositories.size) { index ->
                        val repo = repositories[index]
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clickable { selected = repo }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(repo.name, fontWeight = FontWeight.SemiBold)
                                    Text("Local • Tap to open Git", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                androidx.compose.material3.IconButton(onClick = { editing = repo; name = repo.name; error = null }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                                }
                                androidx.compose.material3.IconButton(onClick = { deleting = repo }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editing != null) {
        val old = editing
        AlertDialog(
            onDismissRequest = { showCreate = false; editing = null; error = null },
            title = { Text(if (old == null) "Create repository" else "Rename repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Repository name") }, singleLine = true)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        if (old == null) {
                            val folder = LocalRepositoryStorage.create(context, name)
                            try { database.insert(name.trim(), folder, System.currentTimeMillis()) }
                            catch (e: Exception) { LocalRepositoryStorage.delete(context, folder); throw e }
                        } else {
                            val folder = LocalRepositoryStorage.rename(context, old.folderName, name)
                            database.rename(old.id, name.trim(), folder)
                        }
                        refresh(); showCreate = false; editing = null; error = null
                    } catch (e: Exception) { error = e.message ?: "Operation failed." }
                }) { Text(if (old == null) "Create" else "Save") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false; editing = null }) { Text("Cancel") } }
        )
    }

    deleting?.let { repo ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${repo.name}?") },
            text = { Text("This permanently deletes this repository and its Git history. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        check(LocalRepositoryStorage.delete(context, repo.folderName))
                        database.delete(repo.id)
                        refresh()
                    } finally { deleting = null }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun GitRepositoryScreen(
    repository: LocalRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val directory = java.io.File(LocalRepositoryStorage.root(context), repository.folderName)
    val manager = remember(repository.id, repository.folderName) { GitRepositoryManager(directory) }
    var initialized by remember { androidx.compose.runtime.mutableStateOf(java.io.File(directory, ".git").isDirectory) }
    var branch by remember { androidx.compose.runtime.mutableStateOf("Not initialized") }
    var branches by remember { androidx.compose.runtime.mutableStateOf(emptyList<String>()) }
    var showCreateBranch by remember { androidx.compose.runtime.mutableStateOf(false) }
    var newBranchName by remember { androidx.compose.runtime.mutableStateOf("") }
    var showDeleteBranch by remember { androidx.compose.runtime.mutableStateOf(false) }
    var branchToDelete by remember { androidx.compose.runtime.mutableStateOf("") }
    var statusText by remember { androidx.compose.runtime.mutableStateOf("Loading status…") }
    var history by remember { androidx.compose.runtime.mutableStateOf(emptyList<com.anb.git.data.GitCommitInfo>()) }
    var commitMessage by remember { androidx.compose.runtime.mutableStateOf("") }
    var error by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var notice by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    fun reload() {
        initialized = java.io.File(directory, ".git").isDirectory
        if (initialized) {
            try {
                val snapshot = manager.status()
                branch = snapshot.branch
                branches = manager.listBranches()
                statusText = if (snapshot.isClean) "Working tree clean" else buildString {
                    appendLine("Staged: ${snapshot.staged.size}")
                    appendLine("Modified: ${snapshot.changed.size}")
                    appendLine("Untracked: ${snapshot.untracked.size}")
                    append("Removed: ${snapshot.removed.size}")
                }
                history = manager.history(30)
            } catch (e: Exception) {
                error = e.message ?: "Could not read Git status."
            }
        } else {
            branch = "Not initialized"
            statusText = "Git repository has not been initialized."
            history = emptyList()
        }
    }
    LaunchedEffect(repository.id) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Repositories") }
            Spacer(Modifier.weight(1f))
            Text(repository.name, fontWeight = FontWeight.Bold)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version Control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Branch: $branch", color = MaterialTheme.colorScheme.primary)
                if (initialized) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Branch manager", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { newBranchName = ""; showCreateBranch = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.size(5.dp))
                            Text("New branch")
                        }
                    }
                    if (branches.isEmpty()) {
                        Text("No local branches yet. Create the first commit to create the initial branch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            branches.forEach { branchName ->
                                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(branchName, fontWeight = FontWeight.Medium)
                                            if (branchName == branch) {
                                                Text("Current branch", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        if (branchName != branch) {
                                            TextButton(onClick = {
                                                try {
                                                    manager.switchBranch(branchName)
                                                    reload()
                                                    notice = "Switched to $branchName."
                                                    error = null
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Could not switch branch. Save or commit local changes first."
                                                }
                                            }) { Text("Switch") }
                                            TextButton(onClick = {
                                                branchToDelete = branchName
                                                showDeleteBranch = true
                                            }) {
                                                Text("Delete", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!initialized) {
                    Button(onClick = {
                        try {
                            manager.initialize()
                            reload()
                            notice = "Git repository initialized."
                            error = null
                        } catch (e: Exception) { error = e.message ?: "Git init failed." }
                    }) { Text("Initialize Git") }
                } else {
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        label = { Text("Commit message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            try {
                                manager.stageAll()
                                reload()
                                notice = "All changes staged."
                                error = null
                            } catch (e: Exception) { error = e.message ?: "Stage failed." }
                        }) { Text("Stage all") }
                        Button(onClick = {
                            try {
                                manager.stageAll()
                                manager.commit(commitMessage)
                                commitMessage = ""
                                reload()
                                notice = "Commit created."
                                error = null
                            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message ?: "No detail"}" }
                        }) { Text("Commit") }
                    }
                    OutlinedButton(onClick = { reload(); error = null }) { Text("Refresh status") }
                }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        Text("Commit history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (history.isEmpty()) {
            Text("No commits yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history.size) { index ->
                    val commit = history[index]
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(commit.message, fontWeight = FontWeight.SemiBold)
                            Text("${commit.shortId} • ${commit.author}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteBranch) {
        AlertDialog(
            onDismissRequest = { showDeleteBranch = false },
            title = { Text("Delete branch?") },
            text = {
                Text("Delete local branch \"$branchToDelete\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        manager.deleteBranch(branchToDelete)
                        showDeleteBranch = false
                        notice = "Branch $branchToDelete deleted."
                        branchToDelete = ""
                        reload()
                        error = null
                    } catch (e: Exception) {
                        error = e.message ?: "Could not delete branch."
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteBranch = false
                    branchToDelete = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (showCreateBranch) {
        AlertDialog(
            onDismissRequest = { showCreateBranch = false },
            title = { Text("Create branch") },
            text = {
                OutlinedTextField(
                    value = newBranchName,
                    onValueChange = { newBranchName = it },
                    label = { Text("Branch name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val created = manager.createBranch(newBranchName)
                        showCreateBranch = false
                        newBranchName = ""
                        reload()
                        notice = "Branch $created created."
                        error = null
                    } catch (e: Exception) {
                        error = e.message ?: "Could not create branch."
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBranch = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CodeEditorScreen() {
    val context = LocalContext.current
    val database = remember { RepositoryDatabase(context.applicationContext) }
    val repositories = remember { mutableStateListOf<LocalRepository>() }
    var selectedRepository by remember { mutableStateOf<LocalRepository?>(null) }
    var repositoryMenu by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(emptyList<java.io.File>()) }
    var selectedFile by remember { mutableStateOf<java.io.File?>(null) }
    var editorText by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var showNewFile by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var dirty by remember { mutableStateOf(false) }

    fun refreshFiles() {
        val repo = selectedRepository ?: return
        val root = java.io.File(LocalRepositoryStorage.root(context), repo.folderName).canonicalFile
        if (!root.isDirectory) {
            files = emptyList()
            return
        }
        files = root.listFiles()
            ?.filter { it.isFile && it.name != ".DS_Store" }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
        val active = selectedFile
        if (active != null && !active.exists()) {
            selectedFile = null
            editorText = ""
            dirty = false
        }
    }

    LaunchedEffect(Unit) {
        repositories.clear()
        repositories.addAll(database.getAll())
        if (selectedRepository == null) selectedRepository = repositories.firstOrNull()
    }
    LaunchedEffect(selectedRepository?.id) {
        selectedFile = null
        editorText = ""
        dirty = false
        refreshFiles()
    }

    val repo = selectedRepository
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Code Editor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (repositories.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No repository available", fontWeight = FontWeight.Bold)
                    Text("Create a local repository first from the Repositories tab.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Box {
                OutlinedButton(onClick = { repositoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(repo?.name ?: "Select repository")
                }
                DropdownMenu(expanded = repositoryMenu, onDismissRequest = { repositoryMenu = false }) {
                    repositories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = { selectedRepository = item; repositoryMenu = false }
                        )
                    }
                }
            }
            if (repo != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Files (${files.size})", fontWeight = FontWeight.SemiBold)
                    Button(onClick = { fileName = ""; error = null; showNewFile = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(5.dp))
                        Text("New file")
                    }
                }
                if (selectedFile == null) {
                    if (files.isEmpty()) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("No files yet")
                                Text("Create a file to begin editing.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(files.size) { index ->
                                val file = files[index]
                                Card(Modifier.fillMaxWidth().clickable {
                                    try {
                                        selectedFile = file
                                        editorText = file.readText()
                                        dirty = false
                                        error = null
                                        message = null
                                    } catch (e: Exception) {
                                        error = e.message ?: "Could not open file."
                                    }
                                }) {
                                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(file.name, Modifier.padding(start = 10.dp), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            if (dirty) {
                                error = "Save or discard your changes before leaving this file."
                            } else {
                                selectedFile = null
                                editorText = ""
                                error = null
                                refreshFiles()
                            }
                        }) { Text("‹ Files") }
                        Text(selectedFile?.name ?: "", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            try {
                                val file = selectedFile ?: throw IllegalStateException("No file selected.")
                                file.writeText(editorText)
                                dirty = false
                                message = "File saved locally."
                                error = null
                                refreshFiles()
                            } catch (e: Exception) {
                                error = e.message ?: "Could not save file."
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.size(5.dp))
                            Text("Save")
                        }
                    }
                    Card(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxSize().padding(10.dp)) {
                            Text(
                                "LOCAL FILE • ${if (dirty) "UNSAVED CHANGES" else "SAVED"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = editorText,
                                onValueChange = { editorText = it; dirty = true; message = null },
                                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                                placeholder = { Text("Write your code here…") }
                            )
                        }
                    }
                }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    if (showNewFile && repo != null) {
        AlertDialog(
            onDismissRequest = { showNewFile = false },
            title = { Text("Create new file") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it; error = null },
                        label = { Text("File name (e.g. main.kt)") },
                        singleLine = true
                    )
                    Text("Files are stored inside this repository's private app folder.", style = MaterialTheme.typography.bodySmall)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val clean = fileName.trim()
                        require(clean.isNotEmpty() && clean.length <= 100) { "Enter a file name up to 100 characters." }
                        require(clean != "." && clean != ".." && clean.none { it == '/' || it == '\\' || it.isISOControl() }) {
                            "Only a file name is allowed. Folder paths are not supported yet."
                        }
                        val root = java.io.File(LocalRepositoryStorage.root(context), repo.folderName).canonicalFile
                        val target = java.io.File(root, clean).canonicalFile
                        require(target.parentFile == root) { "Invalid file path." }
                        check(!target.exists()) { "A file with this name already exists." }
                        check(target.createNewFile()) { "Could not create file." }
                        showNewFile = false
                        fileName = ""
                        refreshFiles()
                        selectedFile = target
                        editorText = ""
                        dirty = false
                        message = "File created. Start editing."
                        error = null
                    } catch (e: Exception) {
                        error = e.message ?: "Could not create file."
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFile = false }) { Text("Cancel") } }
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
        InAppUpdateSection()
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About", fontWeight = FontWeight.SemiBold)
                Text("ANB Git v${com.anb.git.BuildConfig.VERSION_NAME}")
                Text("Offline Git tools with optional secure in-app updates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
