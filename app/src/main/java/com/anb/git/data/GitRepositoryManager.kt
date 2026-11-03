package com.anb.git.data

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.Repository
import java.io.ByteArrayOutputStream
import java.io.File
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.AbstractTreeIterator

data class GitStatusSnapshot(
    val branch: String,
    val staged: List<String>,
    val changed: List<String>,
    val untracked: List<String>,
    val removed: List<String>,
    val isClean: Boolean
)

data class GitCommitInfo(
    val id: String,
    val shortId: String,
    val message: String,
    val author: String,
    val timestamp: Long
)

data class GitFileDiff(
    val path: String,
    val changeType: String,
    val patch: String
)

data class GitDiffSnapshot(
    val staged: List<GitFileDiff>,
    val unstaged: List<GitFileDiff>,
    val untracked: List<String>
)

data class GitCherryPickResult(
    val status: String,
    val conflicts: List<String>,
    val newHead: String?
)

data class GitMergeResult(
    val status: String,
    val conflicts: List<String>,
    val newHead: String?
)

data class GitRevertResult(
    val status: String,
    val conflicts: List<String>,
    val failingPaths: List<String>,
    val newHead: String?
)

/** Local-only Git operations backed by JGit. */
class GitRepositoryManager(private val repositoryDirectory: File) {
    init {
        require(repositoryDirectory.exists() && repositoryDirectory.isDirectory) {
            "Repository directory does not exist."
        }
    }

    fun initialize(): Boolean {
        if (File(repositoryDirectory, ".git").exists()) return false
        Git.init().setDirectory(repositoryDirectory).call().use { }
        return true
    }

    fun status(): GitStatusSnapshot {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val s: Status = git.status().call()
            return GitStatusSnapshot(
                branch = git.repository.branch ?: "HEAD",
                staged = (s.added + s.changed).sorted(),
                changed = s.modified.sorted(),
                untracked = s.untracked.sorted(),
                removed = s.missing.sorted(),
                isClean = s.isClean
            )
        }
    }

    /**
     * Staged = index vs HEAD; unstaged = worktree vs index.
     * Before the first commit there is no HEAD tree, so compare the index
     * against an empty tree instead of asking JGit for a missing HEAD.
     */
    /**
     * Staged = index vs HEAD; unstaged = working tree vs index.
     * Scan and format each diff with the SAME DiffFormatter so its
     * ObjectReader / WorkingTreeIterator content sources remain available.
     */
    fun diffSnapshot(): GitDiffSnapshot {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val staged = stagedDiffs(git)
            val unstaged = unstagedDiffs(git)
            val untracked = git.status().call().untracked.sorted()
            return GitDiffSnapshot(staged = staged, unstaged = unstaged, untracked = untracked)
        }
    }

    private fun stagedDiffs(git: Git): List<GitFileDiff> {
        val repository = git.repository
        val headId = repository.resolve(Constants.HEAD)
            ?: return formatTreeDiff(git, EmptyTreeIterator(), DirCacheIterator(repository.readDirCache()))

        repository.newObjectReader().use { reader ->
            RevWalk(reader).use { walk ->
                val headCommit = walk.parseCommit(headId)
                val headTree = CanonicalTreeParser()
                headTree.reset(reader, headCommit.tree)
                return formatTreeDiff(git, headTree, DirCacheIterator(repository.readDirCache()))
            }
        }
    }

    private fun unstagedDiffs(git: Git): List<GitFileDiff> =
        formatTreeDiff(git, DirCacheIterator(git.repository.readDirCache()), FileTreeIterator(git.repository))

    /**
     * Scan and format with the same formatter so working-tree content sources
     * remain available; formatting with a second formatter can cause
     * MissingObjectException for working-tree-side content.
     */
    private fun formatTreeDiff(
        git: Git,
        oldTree: AbstractTreeIterator,
        newTree: AbstractTreeIterator
    ): List<GitFileDiff> {
        val output = ByteArrayOutputStream()
        val formatter = DiffFormatter(output)
        return try {
            formatter.setRepository(git.repository)
            formatter.setDetectRenames(true)
            val entries = formatter.scan(oldTree, newTree)
            entries.map { entry ->
                output.reset()
                formatter.format(entry)
                formatter.flush()
                val path = if (entry.newPath != DiffEntry.DEV_NULL) entry.newPath else entry.oldPath
                GitFileDiff(
                    path = path,
                    changeType = entry.changeType.name,
                    patch = output.toString(Charsets.UTF_8.name())
                )
            }
        } finally {
            formatter.close()
        }
    }


    /**
     * Merge a local branch into the current branch.
     * Requires a clean worktree. Conflicts are reported and left for Phase 5.4.
     */
    fun mergeBranch(name: String): GitMergeResult {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Select a branch to merge." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val repository = git.repository
            val current = repository.branch ?: "HEAD"
            require(cleanName != current) { "Cannot merge the current branch into itself." }
            check(git.status().call().isClean) {
                "Commit or discard working-tree changes before merging."
            }
            val target = repository.findRef("refs/heads/$cleanName")
                ?: throw IllegalArgumentException("Branch does not exist.")
            repository.config.setString("user", null, "name", "ANB Git")
            repository.config.setString("user", null, "email", "anb-git@local.invalid")
            repository.config.save()
            val result = git.merge()
                .include(target)
                .setCommit(true)
                .call()
            return GitMergeResult(
                status = result.mergeStatus.name,
                conflicts = result.conflicts?.keys?.sorted().orEmpty(),
                newHead = result.newHead?.name
            )
        }
    }



    /**
     * Apply one existing commit to the current branch.
     * Requires a clean worktree. Conflicts are left for the existing resolver.
     */
    fun cherryPickCommit(commitId: String): GitCherryPickResult {
        val cleanId = commitId.trim()
        require(cleanId.isNotEmpty()) { "Select a commit to cherry-pick." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val repository = git.repository
            check(git.status().call().isClean) {
                "Commit or discard working-tree changes before cherry-picking."
            }
            check(!File(repository.directory, "MERGE_HEAD").exists() &&
                !File(repository.directory, "CHERRY_PICK_HEAD").exists()) {
                "Finish or abort the current merge/cherry-pick first."
            }
            val objectId = repository.resolve(cleanId)
                ?: throw IllegalArgumentException("Commit was not found.")
            val commit = RevWalk(repository).use { walk -> walk.parseCommit(objectId) }
            repository.config.setString("user", null, "name", "ANB Git")
            repository.config.setString("user", null, "email", "anb-git@local.invalid")
            repository.config.save()
            val result = git.cherryPick().include(commit).call()
            return GitCherryPickResult(
                status = result.status.name,
                conflicts = if (result.status.name == "CONFLICTING") {
                    conflictFiles()
                } else {
                    emptyList()
                },
                newHead = result.newHead?.name
            )
        }
    }

    /**
     * Revert one non-merge commit on the current branch.
     * Requires a clean worktree and no other in-progress Git operation.
     * Merge commits are rejected until an explicit mainline-parent selector exists.
     */
    fun revertCommit(commitId: String): GitRevertResult {
        val cleanId = commitId.trim()
        require(cleanId.isNotEmpty()) { "Select a commit to revert." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val repository = git.repository
            check(git.status().call().isClean) {
                "Commit or discard working-tree changes before reverting."
            }
            check(!File(repository.directory, "MERGE_HEAD").exists() &&
                !File(repository.directory, "CHERRY_PICK_HEAD").exists() &&
                !File(repository.directory, "REVERT_HEAD").exists()) {
                "Finish or abort the current Git operation first."
            }
            val objectId = repository.resolve(cleanId)
                ?: throw IllegalArgumentException("Commit was not found.")
            val commit = RevWalk(repository).use { walk -> walk.parseCommit(objectId) }
            require(commit.parentCount <= 1) {
                "Reverting a merge commit requires selecting a mainline parent; this is not supported yet."
            }
            repository.config.setString("user", null, "name", "ANB Git")
            repository.config.setString("user", null, "email", "anb-git@local.invalid")
            repository.config.save()
            val result = git.revert().include(commit).call()
            return GitRevertResult(
                status = result.status.name,
                conflicts = result.conflicts?.keys?.sorted().orEmpty(),
                failingPaths = result.failingPaths?.keys?.sorted().orEmpty(),
                newHead = result.newHead?.name
            )
        }
    }

    /** Paths whose index contains unresolved stages (1/2/3). */
    fun conflictFiles(): List<String> {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val index = git.repository.readDirCache()
            return (0 until index.entryCount)
                .map { index.getEntry(it) }
                .filter { it.stage != 0 }
                .map { it.pathString }
                .distinct()
                .sorted()
        }
    }

    /** Resolve a path using stage 2 (current) or stage 3 (incoming). */
    fun resolveConflict(path: String, choice: String) {
        require(choice == "CURRENT" || choice == "INCOMING") { "Unknown conflict choice." }
        val cleanPath = path.trim()
        require(cleanPath.isNotEmpty() && !cleanPath.startsWith("/") &&
            cleanPath.split('/').none { it == ".." || it.isEmpty() }) { "Invalid repository path." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val repository = git.repository
            val index = repository.readDirCache()
            val selectedStage = if (choice == "CURRENT") 2 else 3
            val entry = (0 until index.entryCount)
                .map { index.getEntry(it) }
                .firstOrNull { it.pathString == cleanPath && it.stage == selectedStage }
            if (entry == null) {
                val target = File(repositoryDirectory, cleanPath).canonicalFile
                val root = repositoryDirectory.canonicalFile
                require(target.toPath().startsWith(root.toPath())) { "Invalid repository path." }
                if (target.exists()) target.delete()
                git.rm().addFilepattern(cleanPath).call()
            } else {
                val bytes = repository.newObjectReader().use { reader -> reader.open(entry.objectId).bytes }
                val target = File(repositoryDirectory, cleanPath).canonicalFile
                val root = repositoryDirectory.canonicalFile
                require(target.toPath().startsWith(root.toPath())) { "Invalid repository path." }
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
                git.add().addFilepattern(cleanPath).call()
            }
        }
    }

    /** Stage a manually edited conflict file. */
    fun stageResolvedFile(path: String) {
        val cleanPath = path.trim()
        require(cleanPath.isNotEmpty() && !cleanPath.startsWith("/") &&
            cleanPath.split('/').none { it == ".." || it.isEmpty() }) { "Invalid repository path." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val target = File(repositoryDirectory, cleanPath).canonicalFile
            val root = repositoryDirectory.canonicalFile
            require(target.toPath().startsWith(root.toPath())) { "Invalid repository path." }
            if (target.exists()) git.add().addFilepattern(cleanPath).call()
            else git.rm().addFilepattern(cleanPath).call()
        }
    }

    /** Complete a merge after all unmerged index entries have been resolved. */
    fun continueMerge(message: String): GitCommitInfo {
        val cleanMessage = message.trim()
        require(cleanMessage.isNotEmpty()) { "Enter a merge commit message." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val repository = git.repository
            check(repository.readMergeHeads()?.isNotEmpty() == true ||
                File(repository.directory, "CHERRY_PICK_HEAD").isFile ||
                File(repository.directory, "REVERT_HEAD").isFile) {
                "No merge, cherry-pick or revert is currently in progress."
            }
            val unresolved = conflictFiles()
            check(unresolved.isEmpty()) { "Resolve all conflicts first: ${unresolved.joinToString()}" }
            return git.commit()
                .setMessage(cleanMessage)
                .setAuthor("ANB Git", "anb-git@local.invalid")
                .setCommitter("ANB Git", "anb-git@local.invalid")
                .setAllowEmpty(true)
                .call()
                .toInfo()
        }
    }

    /** Abort an in-progress merge or cherry-pick and restore the current branch HEAD. */
    fun abortMerge() {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val repository = git.repository
            val mergeInProgress = repository.readMergeHeads()?.isNotEmpty() == true
            val cherryPickInProgress = File(repository.directory, "CHERRY_PICK_HEAD").isFile
            val revertInProgress = File(repository.directory, "REVERT_HEAD").isFile
            check(mergeInProgress || cherryPickInProgress || revertInProgress) {
                "No merge, cherry-pick or revert is currently in progress."
            }
            // JGit's HARD reset clears the operation state for the detected repository state.
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                .setRef(Constants.HEAD).call()
            if (mergeInProgress) {
                repository.writeMergeHeads(null)
                repository.writeMergeCommitMsg(null)
            }
            if (revertInProgress) {
                File(repository.directory, "REVERT_HEAD").delete()
                File(repository.directory, "REVERT_MSG").delete()
            }
        }
    }

    fun listBranches(): List<String> {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            return git.branchList().call()
                .filter { it.name.startsWith("refs/heads/") }
                .map { it.name.removePrefix("refs/heads/") }
                .sorted()
        }
    }

    fun currentBranch(): String {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            return git.repository.branch ?: "HEAD"
        }
    }

    fun createBranch(name: String): String {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Branch name cannot be empty." }
        require(Repository.isValidRefName("refs/heads/$cleanName")) { "Branch name is invalid." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            check(git.repository.resolve("refs/heads/$cleanName") == null) {
                "A branch with this name already exists."
            }
            git.branchCreate().setName(cleanName).call()
        }
        return cleanName
    }

    fun switchBranch(name: String): String {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Select a branch first." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            check(git.repository.resolve("refs/heads/$cleanName") != null) { "Branch does not exist." }
            git.checkout().setName(cleanName).call()
            return git.repository.branch ?: cleanName
        }
    }

    fun deleteBranch(name: String) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Select a branch first." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            check(git.repository.branch != cleanName) {
                "Switch to another branch before deleting the current branch."
            }
            check(git.repository.resolve("refs/heads/$cleanName") != null) { "Branch does not exist." }
            git.branchDelete().setBranchNames(cleanName).setForce(false).call()
        }
    }

    fun stageAll() {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            git.add().addFilepattern(".").call()
            git.add().setUpdate(true).addFilepattern(".").call()
        }
    }

    fun commit(message: String): GitCommitInfo {
        val cleanMessage = message.trim()
        require(cleanMessage.isNotEmpty()) { "Commit message cannot be empty." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            check(!git.status().call().isClean) { "There are no changes to commit." }
            return git.commit()
                .setMessage(cleanMessage)
                .setAuthor("ANB Git", "anb-git@local.invalid")
                .setCommitter("ANB Git", "anb-git@local.invalid")
                .setAllowEmpty(false)
                .call()
                .toInfo()
        }
    }

    fun history(limit: Int = 50): List<GitCommitInfo> {
        ensureInitialized()
        require(limit in 1..200)
        Git.open(repositoryDirectory).use { git ->
            if (git.repository.resolve("HEAD") == null) return emptyList()
            return git.log().setMaxCount(limit).call().map { it.toInfo() }.toList()
        }
    }

    private fun ensureInitialized() {
        check(File(repositoryDirectory, ".git").isDirectory) {
            "Git is not initialized. Initialize this repository first."
        }
    }

    private fun RevCommit.toInfo() = GitCommitInfo(
        id = name,
        shortId = name.take(7),
        message = fullMessage.trim(),
        author = authorIdent?.name ?: "Unknown",
        timestamp = commitTime.toLong() * 1000L
    )
}
