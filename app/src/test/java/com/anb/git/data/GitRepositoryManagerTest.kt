package com.anb.git.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GitRepositoryManagerTest {
    @Test
    fun diffSnapshotSeparatesStagedAndUnstagedChanges() {
        val root = Files.createTempDirectory("anb-git-diff-test").toFile()
        try {
            val manager = GitRepositoryManager(root)
            manager.initialize()
            val sample = File(root, "sample.txt")
            sample.writeText("first line\n")
            manager.stageAll()
            val initialDiff = manager.diffSnapshot()
            assertTrue(initialDiff.staged.any { it.path == "sample.txt" && it.changeType == "ADD" })
            assertTrue(initialDiff.unstaged.isEmpty())
            assertTrue(initialDiff.untracked.isEmpty())
            manager.commit("Initial test commit")
            sample.writeText("first line\nsecond line\n")
            val unstaged = manager.diffSnapshot()
            assertTrue(unstaged.unstaged.any { it.path == "sample.txt" && it.patch.contains("+second line") })
            assertTrue(unstaged.staged.isEmpty())
            manager.stageAll()
            val staged = manager.diffSnapshot()
            assertTrue(staged.staged.any { it.path == "sample.txt" && it.patch.contains("+second line") })
            assertTrue(staged.unstaged.isEmpty())
            assertTrue(staged.untracked.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun mergeBranchFastForwardsAndIncludesFeatureFile() {
        val root = Files.createTempDirectory("anb-git-merge-ff").toFile()
        try {
            val manager = GitRepositoryManager(root)
            manager.initialize()
            File(root, "base.txt").writeText("base\n")
            manager.stageAll()
            manager.commit("Base commit")
            manager.createBranch("feature")
            manager.switchBranch("feature")
            File(root, "feature.txt").writeText("feature work\n")
            manager.stageAll()
            manager.commit("Feature commit")
            manager.switchBranch("master")
            val result = manager.mergeBranch("feature")
            assertTrue(result.status == "FAST_FORWARD" || result.status == "MERGED")
            assertTrue(result.conflicts.isEmpty())
            assertTrue(File(root, "feature.txt").readText().contains("feature work"))
            assertTrue(manager.history().any { it.message == "Feature commit" })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun mergeBranchReportsConflictingFile() {
        val root = Files.createTempDirectory("anb-git-merge-conflict").toFile()
        try {
            val manager = GitRepositoryManager(root)
            manager.initialize()
            val sample = File(root, "shared.txt")
            sample.writeText("base line\n")
            manager.stageAll()
            manager.commit("Base commit")
            manager.createBranch("feature")
            sample.writeText("master version\n")
            manager.stageAll()
            manager.commit("Master change")
            manager.switchBranch("feature")
            sample.writeText("feature version\n")
            manager.stageAll()
            manager.commit("Feature change")
            manager.switchBranch("master")
            val result = manager.mergeBranch("feature")
            assertEquals("CONFLICTING", result.status)
            assertTrue(result.conflicts.contains("shared.txt"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun resolveConflictChoosesCurrentAndCompletesMerge() {
        val root = Files.createTempDirectory("anb-git-resolve-current").toFile()
        try {
            val manager = GitRepositoryManager(root)
            manager.initialize()
            val sample = File(root, "shared.txt")
            sample.writeText("base\n")
            manager.stageAll()
            manager.commit("Base commit")
            manager.createBranch("feature")
            sample.writeText("current version\n")
            manager.stageAll()
            manager.commit("Current change")
            manager.switchBranch("feature")
            sample.writeText("incoming version\n")
            manager.stageAll()
            manager.commit("Incoming change")
            manager.switchBranch("master")
            val merge = manager.mergeBranch("feature")
            assertEquals("CONFLICTING", merge.status)
            assertTrue(manager.conflictFiles().contains("shared.txt"))
            manager.resolveConflict("shared.txt", "CURRENT")
            assertEquals("current version\n", sample.readText())
            assertTrue(manager.conflictFiles().isEmpty())
            val commit = manager.continueMerge("Resolve shared file")
            assertEquals("Resolve shared file", commit.message)
            assertTrue(manager.status().isClean)
            assertTrue(manager.history().any { it.message == "Resolve shared file" })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun abortMergeRestoresCurrentBranchAndClearsConflictState() {
        val root = Files.createTempDirectory("anb-git-abort-merge").toFile()
        try {
            val manager = GitRepositoryManager(root)
            manager.initialize()
            val sample = File(root, "shared.txt")
            sample.writeText("base\n")
            manager.stageAll()
            manager.commit("Base commit")
            manager.createBranch("feature")
            sample.writeText("current version\n")
            manager.stageAll()
            manager.commit("Current change")
            manager.switchBranch("feature")
            sample.writeText("incoming version\n")
            manager.stageAll()
            manager.commit("Incoming change")
            manager.switchBranch("master")
            assertEquals("CONFLICTING", manager.mergeBranch("feature").status)
            manager.abortMerge()
            assertEquals("current version\n", sample.readText())
            assertTrue(manager.conflictFiles().isEmpty())
            assertTrue(manager.status().isClean)
        } finally {
            root.deleteRecursively()
        }
    }


    @Test
    fun cherryPickAppliesCommitFromAnotherBranch() {
        val root = Files.createTempDirectory("anb-git-cherry-pick").toFile()
        try {
            val manager = GitRepositoryManager(root)
            manager.initialize()
            File(root, "base.txt").writeText("base\n")
            manager.stageAll()
            manager.commit("Base commit")
            manager.createBranch("feature")
            manager.switchBranch("feature")
            File(root, "feature.txt").writeText("feature content\n")
            manager.stageAll()
            val featureCommit = manager.commit("Feature commit")
            manager.switchBranch("master")
            val result = manager.cherryPickCommit(featureCommit.id)
            assertEquals("OK", result.status)
            assertTrue(result.conflicts.isEmpty())
            assertEquals("feature content\n", File(root, "feature.txt").readText())
            assertTrue(manager.history().any { it.message == "Feature commit" })
            assertTrue(manager.status().isClean)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun cherryPickRequiresCleanWorkingTree() {
        val root = Files.createTempDirectory("anb-git-cherry-pick-dirty").toFile()
        try {
            val manager = GitRepositoryManager(root)
            manager.initialize()
            File(root, "base.txt").writeText("base\n")
            manager.stageAll()
            val commit = manager.commit("Base commit")
            File(root, "base.txt").writeText("uncommitted change\n")
            try {
                manager.cherryPickCommit(commit.id)
                throw AssertionError("Expected dirty working tree to be rejected.")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("working-tree changes"))
            }
        } finally {
            root.deleteRecursively()
        }
    }

}
