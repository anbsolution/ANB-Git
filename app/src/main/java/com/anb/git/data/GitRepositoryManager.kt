package com.anb.git.data

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
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

    /** Non-forced checkout; JGit rejects changes that would be overwritten. */
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

    /** Only deletes a non-current branch, without force. */
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
            return git.commit().setMessage(cleanMessage).setAllowEmpty(false).call().toInfo()
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
