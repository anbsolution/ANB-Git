package com.anb.git.ui

import com.anb.git.ui.theme.SuccessGreen

import androidx.compose.foundation.layout.heightIn

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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("ANB Git", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Developer workspace",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ANB GIT • DEVELOPER STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Bold)
                    Text("Your workspace",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Text("Code, manage repositories and keep your work organized from one mobile workspace.",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(50)))
                        Text("LOCAL-FIRST WORKSPACE",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }
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
                        Text("LOCAL WORKSPACE", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                    Text("Your projects stay on this device", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Project files and Git history are stored in ANB Git private app storage.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(8.dp).background(SuccessGreen, RoundedCornerShape(50)))
                        Text("Offline-ready", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("Offline-first", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("Work on local projects", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Git-powered", fontWeight = FontWeight.Bold)
                        Text("Track your code changes", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Built for mobile development", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Manage local repositories, edit project files and use Git tools with a consistent royal theme.",
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
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Your repositories", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        Text("${repositories.size} local projects",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f))
                    }
                    Button(
                        onClick = { name = ""; error = null; showCreate = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(5.dp))
                        Text("New")
                    }
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
                        Card(
                            Modifier.fillMaxWidth().clickable { selected = repo },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(44.dp).background(
                                        if (index % 2 == 0) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.tertiaryContainer,
                                        RoundedCornerShape(14.dp)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (index % 2 == 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(repo.name, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("LOCAL PROJECT  •  Open Git workspace",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun GitRepositoryScreen(repository: LocalRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val directory = java.io.File(LocalRepositoryStorage.root(context), repository.folderName)
    val manager = remember(repository.id, repository.folderName) { GitRepositoryManager(directory) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
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
    var busy by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDiffViewer by remember { androidx.compose.runtime.mutableStateOf(false) }
    var diffSnapshot by remember { mutableStateOf<com.anb.git.data.GitDiffSnapshot?>(null) }
    var showMergeDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var mergeBranchName by remember { androidx.compose.runtime.mutableStateOf("") }
    var showConflictDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var conflictPaths by remember { androidx.compose.runtime.mutableStateOf(emptyList<String>()) }
    var selectedConflict by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var conflictContent by remember { androidx.compose.runtime.mutableStateOf("") }
    var mergeCommitMessage by remember { androidx.compose.runtime.mutableStateOf("Merge branch") }



    suspend fun reload() {
        try {
            val data = withContext(Dispatchers.IO) {
                if (!java.io.File(directory, ".git").isDirectory) null
                else Triple(manager.status(), manager.listBranches(), manager.history(30))
            }
            initialized = data != null
            if (data == null) {
                branch = "Not initialized"
                branches = emptyList()
                statusText = "Git repository has not been initialized."
                history = emptyList()
            } else {
                val st = data.first
                branch = st.branch
                branches = data.second
                statusText = if (st.isClean) "Working tree clean" else buildString {
                    appendLine("Staged: ${st.staged.size}")
                    appendLine("Modified: ${st.changed.size}")
                    appendLine("Untracked: ${st.untracked.size}")
                    append("Removed: ${st.removed.size}")
                }
                history = data.third
            }
        } catch (e: Exception) {
            error = "${e.javaClass.simpleName}: ${e.message ?: "Could not read Git status."}"
        }
    }
    LaunchedEffect(repository.id) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.size(4.dp))
                Text("Repositories")
            }
            Spacer(Modifier.weight(1f))
            Text(repository.name, fontWeight = FontWeight.Bold)
        }
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Git Workspace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("BRANCH  $branch", color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    }
                    if (initialized) {
                        Text("● READY", color = SuccessGreen, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }
                if (initialized) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Branch manager", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(enabled = !busy, onClick = { newBranchName = ""; showCreateBranch = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.size(5.dp)); Text("New branch")
                        }
                    }
                    if (branches.isEmpty()) {
                        Text("No local branches yet. Create the first commit to create the initial branch.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            branches.forEach { name ->
                                Card(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (name == branch)
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(name, fontWeight = FontWeight.Medium)
                                            if (name == branch) Text("Current branch",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (name != branch) {
                                            TextButton(enabled = !busy, onClick = {
                                                scope.launch {
                                                    busy = true
                                                    try {
                                                        withContext(Dispatchers.IO) { manager.switchBranch(name) }
                                                        reload(); notice = "Switched to $name."; error = null
                                                    } catch (e: Exception) {
                                                        error = "${e.javaClass.simpleName}: ${e.message ?: "Could not switch branch."}"
                                                    } finally { busy = false }
                                                }
                                            }) { Text("Switch") }
                                            TextButton(enabled = !busy, onClick = {
                                                branchToDelete = name; showDeleteBranch = true
                                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("REPOSITORY STATUS", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (!initialized) {
                    Button(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            try {
                                withContext(Dispatchers.IO) { manager.initialize() }
                                reload(); notice = "Git repository initialized."; error = null
                            } catch (e: Exception) {
                                error = "${e.javaClass.simpleName}: ${e.message ?: "Git init failed."}"
                            } finally { busy = false }
                        }
                    }) { Text(if (busy) "Please wait…" else "Initialize Git") }
                } else {
                    OutlinedTextField(value = commitMessage, onValueChange = { commitMessage = it },
                        label = { Text("Commit message") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    withContext(Dispatchers.IO) { manager.stageAll() }
                                    reload(); notice = "All changes staged."; error = null
                                } catch (e: Exception) {
                                    error = "${e.javaClass.simpleName}: ${e.message ?: "Stage failed."}"
                                } finally { busy = false }
                            }
                        }) { Text("Stage all") }
                        Button(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val msg = commitMessage
                                    withContext(Dispatchers.IO) { manager.stageAll(); manager.commit(msg) }
                                    commitMessage = ""
                                    reload(); notice = "Commit created."; error = null
                                } catch (e: Exception) {
                                    error = "${e.javaClass.simpleName}: ${e.message ?: "Commit failed."}"
                                } finally { busy = false }
                            }
                        }) { Text(if (busy) "Working…" else "Commit") }
                    }
                    OutlinedButton(
                        enabled = !busy && branches.any { it != branch },
                        onClick = {
                            mergeBranchName = branches.firstOrNull { it != branch }.orEmpty()
                            showMergeDialog = true
                        }
                    ) { Text("Merge branch") }
                    OutlinedButton(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            try {
                                diffSnapshot = withContext(Dispatchers.IO) { manager.diffSnapshot() }
                                showDiffViewer = true
                                error = null
                            } catch (e: Exception) {
                                error = "${e.javaClass.simpleName}: ${e.message ?: "Could not load diff."}"
                            } finally { busy = false }
                        }
                    }) { Text("View diff") }
                    OutlinedButton(enabled = !busy, onClick = {
                        scope.launch { busy = true; try { reload(); error = null } finally { busy = false } }
                    }) { Text("Refresh status") }
                }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("COMMIT TIMELINE", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Bold)
                    Text("Commit history", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
                Box(Modifier.background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(50))
                    .padding(horizontal = 11.dp, vertical = 6.dp)) {
                    Text("${history.size} shown", color = MaterialTheme.colorScheme.onTertiary,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (history.isEmpty()) Text("No commits yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history.size) { i ->
                val c = history[i]
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(c.message, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                        Text("${c.shortId} • ${c.author}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val result = withContext(Dispatchers.IO) { manager.cherryPickCommit(c.id) }
                                    reload()
                                    if (result.conflicts.isNotEmpty()) {
                                        conflictPaths = withContext(Dispatchers.IO) { manager.conflictFiles() }
                                        selectedConflict = conflictPaths.firstOrNull()
                                        conflictContent = selectedConflict?.let { path ->
                                            val file = java.io.File(directory, path)
                                            if (file.isFile) file.readText() else ""
                                        } ?: ""
                                        mergeCommitMessage = "Cherry-pick ${c.shortId}: ${c.message.lineSequence().firstOrNull().orEmpty()}"
                                        showConflictDialog = true
                                        notice = "Cherry-pick paused. Resolve ${result.conflicts.size} conflicting file(s)."
                                    } else {
                                        notice = "Cherry-pick ${result.status.lowercase().replace('_', ' ')}."
                                    }
                                    error = null
                                } catch (e: Exception) {
                                    error = "${e.javaClass.simpleName}: ${e.message ?: "Cherry-pick failed."}"
                                } finally { busy = false }
                            }
                        }) { Text("Cherry-pick") }
                        TextButton(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val result = withContext(Dispatchers.IO) { manager.revertCommit(c.id) }
                                    reload()
                                    if (result.conflicts.isNotEmpty()) {
                                        conflictPaths = withContext(Dispatchers.IO) { manager.conflictFiles() }
                                        selectedConflict = conflictPaths.firstOrNull()
                                        conflictContent = selectedConflict?.let { path ->
                                            val file = java.io.File(directory, path)
                                            if (file.isFile) file.readText() else ""
                                        } ?: ""
                                        mergeCommitMessage = "Revert ${c.shortId}: ${c.message.lineSequence().firstOrNull().orEmpty()}"
                                        showConflictDialog = true
                                        notice = "Revert paused. Resolve ${result.conflicts.size} conflicting file(s)."
                                    } else {
                                        notice = "Revert ${result.status.lowercase().replace('_', ' ')}."
                                    }
                                    error = null
                                } catch (e: Exception) {
                                    error = "${e.javaClass.simpleName}: ${e.message ?: "Revert failed."}"
                                } finally { busy = false }
                            }
                        }) { Text("Revert") }
                    }
                }
            }
        }



    if (showConflictDialog) {
        MergeConflictDialog(
            paths = conflictPaths,
            selectedPath = selectedConflict,
            content = conflictContent,
            mergeMessage = mergeCommitMessage,
            busy = busy,
            onSelect = { path ->
                selectedConflict = path
                conflictContent = try {
                    val file = java.io.File(directory, path)
                    if (file.isFile) file.readText() else ""
                } catch (_: Exception) { "" }
            },
            onContentChange = { conflictContent = it },
            onKeep = { choice ->
                val path = selectedConflict ?: return@MergeConflictDialog
                scope.launch {
                    busy = true
                    try {
                        withContext(Dispatchers.IO) { manager.resolveConflict(path, choice) }
                        conflictPaths = withContext(Dispatchers.IO) { manager.conflictFiles() }
                        selectedConflict = conflictPaths.firstOrNull()
                        conflictContent = selectedConflict?.let { p ->
                            val f = java.io.File(directory, p)
                            if (f.isFile) f.readText() else ""
                        } ?: ""
                        notice = "Conflict choice applied to $path."
                        error = null
                    } catch (e: Exception) {
                        error = e.message ?: "Could not resolve conflict."
                    } finally { busy = false }
                }
            },
            onSaveManual = {
                val path = selectedConflict ?: return@MergeConflictDialog
                scope.launch {
                    busy = true
                    try {
                        withContext(Dispatchers.IO) {
                            val target = java.io.File(directory, path).canonicalFile
                            val root = directory.canonicalFile
                            require(target.toPath().startsWith(root.toPath())) { "Invalid repository path." }
                            target.parentFile?.mkdirs()
                            target.writeText(conflictContent)
                            manager.stageResolvedFile(path)
                        }
                        conflictPaths = withContext(Dispatchers.IO) { manager.conflictFiles() }
                        selectedConflict = conflictPaths.firstOrNull()
                        conflictContent = selectedConflict?.let { p ->
                            val f = java.io.File(directory, p)
                            if (f.isFile) f.readText() else ""
                        } ?: ""
                        notice = "Manual resolution staged."
                        error = null
                    } catch (e: Exception) {
                        error = e.message ?: "Could not stage manual resolution."
                    } finally { busy = false }
                }
            },
            onMergeMessageChange = { mergeCommitMessage = it },
            onContinue = {
                scope.launch {
                    busy = true
                    try {
                        withContext(Dispatchers.IO) { manager.continueMerge(mergeCommitMessage) }
                        showConflictDialog = false
                        reload()
                        notice = "Operation completed and commit created."
                        error = null
                    } catch (e: Exception) {
                        error = e.message ?: "Could not continue merge."
                    } finally { busy = false }
                }
            },
            onAbort = {
                scope.launch {
                    busy = true
                    try {
                        withContext(Dispatchers.IO) { manager.abortMerge() }
                        showConflictDialog = false
                        reload()
                        notice = "Merge / cherry-pick aborted. Current branch restored."
                        error = null
                    } catch (e: Exception) {
                        error = e.message ?: "Could not abort merge."
                    } finally { busy = false }
                }
            },
            onDismiss = { if (!busy) showConflictDialog = false }
        )
    }

    if (showDiffViewer) {
        DiffViewerDialog(snapshot = diffSnapshot, onDismiss = { showDiffViewer = false })
    }

    if (showMergeDialog) AlertDialog(
        onDismissRequest = { if (!busy) showMergeDialog = false },
        title = { Text("Merge local branch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Merge the selected branch into $branch. The working tree must be clean.")
                OutlinedTextField(
                    value = mergeBranchName,
                    onValueChange = { mergeBranchName = it },
                    label = { Text("Branch to merge") },
                    singleLine = true
                )
                Text(
                    "Conflicting files will be reported. Conflict resolution is planned for Phase 5.4.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && mergeBranchName.isNotBlank(), onClick = {
                scope.launch {
                    busy = true
                    try {
                        val result = withContext(Dispatchers.IO) { manager.mergeBranch(mergeBranchName) }
                        showMergeDialog = false
                        reload()
                        if (result.conflicts.isNotEmpty()) {
                            conflictPaths = withContext(Dispatchers.IO) { manager.conflictFiles() }
                            selectedConflict = conflictPaths.firstOrNull()
                            conflictContent = selectedConflict?.let { path ->
                                val file = java.io.File(directory, path)
                                if (file.isFile) file.readText() else ""
                            } ?: ""
                            showConflictDialog = true
                            notice = "Merge paused. Resolve ${result.conflicts.size} conflicting file(s)."
                        } else {
                            notice = "Merge ${result.status.lowercase().replace('_', ' ')}."
                        }
                        error = null
                    } catch (e: Exception) {
                        error = "${e.javaClass.simpleName}: ${e.message ?: "Merge failed."}"
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Merging…" else "Merge") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { showMergeDialog = false }) { Text("Cancel") }
        }
    )


    if (showCreateBranch) AlertDialog(
        onDismissRequest = { showCreateBranch = false },
        title = { Text("Create branch") },
        text = { OutlinedTextField(value = newBranchName, onValueChange = { newBranchName = it },
            label = { Text("Branch name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                try {
                    val n = newBranchName
                    withContext(Dispatchers.IO) { manager.createBranch(n) }
                    showCreateBranch = false; newBranchName = ""
                    reload(); notice = "Branch created."; error = null
                } catch (e: Exception) {
                    error = "${e.javaClass.simpleName}: ${e.message ?: "Could not create branch."}"
                } finally { busy = false }
            }
        }) { Text("Create") } },
        dismissButton = { TextButton(onClick = { showCreateBranch = false }) { Text("Cancel") } }
    )

    if (showDeleteBranch) AlertDialog(
        onDismissRequest = { showDeleteBranch = false },
        title = { Text("Delete branch?") },
        text = { Text("Delete local branch \"$branchToDelete\"? This cannot be undone.") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                try {
                    val n = branchToDelete
                    withContext(Dispatchers.IO) { manager.deleteBranch(n) }
                    showDeleteBranch = false; branchToDelete = ""
                    reload(); notice = "Branch $n deleted."; error = null
                } catch (e: Exception) {
                    error = "${e.javaClass.simpleName}: ${e.message ?: "Could not delete branch."}"
                } finally { busy = false }
            }
        }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDeleteBranch = false; branchToDelete = "" }) { Text("Cancel") } }
    )
}
}


@Composable
private fun MergeConflictDialog(
    paths: List<String>,
    selectedPath: String?,
    content: String,
    mergeMessage: String,
    busy: Boolean,
    onSelect: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onKeep: (String) -> Unit,
    onSaveManual: () -> Unit,
    onMergeMessageChange: (String) -> Unit,
    onContinue: () -> Unit,
    onAbort: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Resolve conflicts", fontWeight = FontWeight.Bold)
                Text("MERGE • CHERRY-PICK • REVERT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Unresolved files: ${paths.size}", fontWeight = FontWeight.SemiBold)
                if (paths.isEmpty()) {
                    Text("All conflict entries are resolved. Continue the merge or abort it.")
                } else {
                    paths.forEach { path ->
                        OutlinedButton(
                            enabled = !busy,
                            onClick = { onSelect(path) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (path == selectedPath) "● $path" else path) }
                    }
                    selectedPath?.let { path ->
                        Text("Selected: $path", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = content,
                            onValueChange = onContentChange,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 260.dp),
                            label = { Text("Manual resolution") },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace)
                        )
                        Text("Choose a side or edit the text and stage your manual resolution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(enabled = !busy, onClick = { onKeep("CURRENT") }) { Text("Keep Current") }
                            OutlinedButton(enabled = !busy, onClick = { onKeep("INCOMING") }) { Text("Keep Incoming") }
                        }
                        Button(enabled = !busy, onClick = onSaveManual, modifier = Modifier.fillMaxWidth()) {
                            Text("Save & Stage Manual Edit")
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = mergeMessage,
                    onValueChange = onMergeMessageChange,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Merge commit message") },
                    singleLine = true
                )
                Button(
                    enabled = !busy && paths.isEmpty() && mergeMessage.isNotBlank(),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue Merge") }
                OutlinedButton(enabled = !busy, onClick = onAbort, modifier = Modifier.fillMaxWidth()) {
                    Text("Abort Merge / Cherry-pick / Revert")
                }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DiffViewerDialog(
    snapshot: com.anb.git.data.GitDiffSnapshot?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diff Viewer") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (snapshot == null) {
                    Text("No diff data available.")
                } else {
                    Text("STAGED CHANGES  •  ${snapshot.staged.size}",
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall)
                    if (snapshot.staged.isEmpty()) {
                        Text("No staged changes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else snapshot.staged.forEach { file ->
                        Text("${file.changeType} • ${file.path}", fontWeight = FontWeight.SemiBold)
                        Text(file.patch, fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("UNSTAGED CHANGES  •  ${snapshot.unstaged.size}",
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall)
                    if (snapshot.unstaged.isEmpty()) {
                        Text("No unstaged changes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else snapshot.unstaged.forEach { file ->
                        Text("${file.changeType} • ${file.path}", fontWeight = FontWeight.SemiBold)
                        Text(file.patch, fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("Untracked files (${snapshot.untracked.size})", fontWeight = FontWeight.Bold)
                    if (snapshot.untracked.isEmpty()) {
                        Text("No untracked files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        snapshot.untracked.forEach { path ->
                            Text("UNTRACKED • $path", color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Stage an untracked file to view its content diff.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
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
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CODE STUDIO", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold)
                Text("Code Editor", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Text("Browse and edit files in your local projects.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f))
            }
        }
        if (repositories.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No repository available", fontWeight = FontWeight.Bold)
                    Text("Create a local repository first from the Repositories tab.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Box {
                OutlinedButton(
                    onClick = { repositoryMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
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
                    Card(
                        Modifier.weight(1f).fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("LOCAL FILE", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(if (dirty) "UNSAVED CHANGES" else "SAVED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dirty) MaterialTheme.colorScheme.error else SuccessGreen,
                                    fontWeight = FontWeight.Bold)
                            }
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
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("ANB GIT • PREFERENCES", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f))
                Text("Settings", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Text("Personalize your development workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Appearance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Royal Pro • Clean White foundation")
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error).forEach { swatch ->
                        Box(Modifier.weight(1f).height(34.dp).background(swatch, RoundedCornerShape(9.dp)))
                    }
                }
                Text("Royal Blue • Clean White • Royal Yellow • Royal Red",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storage & identity", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text("Private app storage")
                Text("Package: com.anb.git", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Your existing app identity and local project storage remain unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
