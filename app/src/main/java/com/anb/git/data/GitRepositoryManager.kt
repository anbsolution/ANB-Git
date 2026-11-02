package com.anb.git.data

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
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

/**
 * Local-only Git operations backed by JGit.
 * Each operation opens and closes its own Git handle.
 */
class GitRepositoryManager(private val repositoryDirectory: File) {

    init {
        require(repositoryDirectory.exists() && repositoryDirectory.isDirectory) {
            "Repository directory does not exist."
        }
    }

    fun initialize(): Boolean {
        val gitDirectory = File(repositoryDirectory, ".git")
        if (gitDirectory.exists()) return false
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

    fun stageAll() {
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            git.add().addFilepattern(".").call()
            // Stage tracked deletions as well as new and modified files.
            git.add().setUpdate(true).addFilepattern(".").call()
        }
    }

    fun commit(message: String): GitCommitInfo {
        val cleanMessage = message.trim()
        require(cleanMessage.isNotEmpty()) { "Commit message cannot be empty." }
        ensureInitialized()
        Git.open(repositoryDirectory).use { git ->
            val status = git.status().call()
            check(!status.isClean) { "There are no changes to commit." }
            val commit = git.commit()
                .setMessage(cleanMessage)
                .setAllowEmpty(false)
                .call()
            return commit.toInfo()
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

    private fun RevCommit.toInfo(): GitCommitInfo = GitCommitInfo(
        id = name,
        shortId = name.take(7),
        message = fullMessage.trim(),
        author = authorIdent?.name ?: "Unknown",
        timestamp = commitTime.toLong() * 1000L
    )
}
