package com.anb.git.data

import android.content.Context
import java.io.File

/**
 * App-private storage bootstrap.
 * No storage permission is needed because files stay under filesDir.
 */
object LocalRepositoryStorage {
    private const val ROOT_NAME = "repositories"

    fun initialize(context: Context): File {
        val root = File(context.filesDir, ROOT_NAME)
        if (!root.exists() && !root.mkdirs()) {
            throw IllegalStateException("Could not create private repository directory")
        }
        return root
    }

    fun root(context: Context): File = File(context.filesDir, ROOT_NAME)
}
