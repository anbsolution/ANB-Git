package com.anb.git.data

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
            assertTrue(unstaged.unstaged.any {
                it.path == "sample.txt" && it.patch.contains("+second line")
            })
            assertTrue(unstaged.staged.isEmpty())

            manager.stageAll()
            val staged = manager.diffSnapshot()
            assertTrue(staged.staged.any {
                it.path == "sample.txt" && it.patch.contains("+second line")
            })
            assertTrue(staged.unstaged.isEmpty())
            assertTrue(staged.untracked.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
