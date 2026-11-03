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
    fun diffSnapshot(): GitDiffSnapshot {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val repository = git.repository
            val stagedEntries = stagedDiffEntries(git)
            val unstagedEntries = unstagedDiffEntries(git)
            val untracked = git.status().call().untracked.sorted()
            return GitDiffSnapshot(
                staged = formatDiffs(git, stagedEntries),
                unstaged = formatDiffs(git, unstagedEntries),
                untracked = untracked
            )
        }
    }

    private fun stagedDiffEntries(git: Git): List<DiffEntry> {
        val repository = git.repository
        if (repository.resolve(Constants.HEAD) != null) {
            return git.diff().setCached(true).call()
        }

        val formatter = DiffFormatter(ByteArrayOutputStream())
        return try {
            formatter.setRepository(repository)
            formatter.setDetectRenames(true)
            formatter.scan(
                EmptyTreeIterator(),
                DirCacheIterator(repository.readDirCache())
            )
        } finally {
            formatter.close()
        }
    }

    /**
     * Compare the index directly with the working tree. This avoids JGit's
     * default diff tree selection, which may resolve HEAD in edge cases.
     */
    private fun unstagedDiffEntries(git: Git): List<DiffEntry> {
        val repository = git.repository
        val formatter = DiffFormatter(ByteArrayOutputStream())
        return try {
            formatter.setRepository(repository)
            formatter.setDetectRenames(true)
            formatter.scan(
                DirCacheIterator(repository.readDirCache()),
                FileTreeIterator(repository)
            )
        } finally {
            formatter.close()
        }
    }

    private fun formatDiffs(git: Git, entries: List<DiffEntry>): List<GitFileDiff> =
        entries.map { entry ->
            val output = ByteArrayOutputStream()
            val formatter = DiffFormatter(output)
            try {
                formatter.setRepository(git.repository)
                formatter.setDetectRenames(true)
                formatter.format(entry)
                formatter.flush()
            } finally {
                formatter.close()
            }
            val path = if (entry.newPath != DiffEntry.DEV_NULL) entry.newPath else entry.oldPath
            GitFileDiff(
                path = path,
                changeType = entry.changeType.name,
                patch = output.toString(Charsets.UTF_8.name())
            )
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
