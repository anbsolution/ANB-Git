package com.anb.git.data

import android.content.Context
import java.io.File

object LocalRepositoryStorage {
    fun root(context: Context): File {
        val directory = File(context.filesDir, "repositories")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Could not create private repository directory.")
        }
        return directory.canonicalFile
    }

    private fun validateName(input: String): String {
        val name = input.trim()
        require(
            name.length in 1..50 &&
                name != "." &&
                name != ".." &&
                name.none { it == '/' || it == '\\' || it.isISOControl() }
        ) { "Use 1–50 characters. Slash and backslash are not allowed." }
        return name
    }

    fun create(context: Context, input: String): String {
        val name = validateName(input)
        val folder = File(root(context), name)
        check(!folder.exists()) { "A repository with this name already exists." }
        check(folder.mkdir()) { "Could not create repository folder." }
        return name
    }

    fun rename(context: Context, oldFolderName: String, input: String): String {
        val newName = validateName(input)
        val base = root(context)
        val oldFolder = File(base, oldFolderName).canonicalFile
        val newFolder = File(base, newName)
        require(oldFolder.parentFile == base) { "Invalid repository path." }
        check(oldFolder.isDirectory) { "Repository folder is missing." }
        check(!newFolder.exists()) { "A repository with this name already exists." }
        check(oldFolder.renameTo(newFolder)) { "Could not rename repository folder." }
        return newName
    }

    fun delete(context: Context, folderName: String): Boolean {
        val base = root(context)
        val target = File(base, folderName).canonicalFile
        require(target.parentFile == base) { "Invalid repository path." }
        return !target.exists() || target.deleteRecursively()
    }
}
