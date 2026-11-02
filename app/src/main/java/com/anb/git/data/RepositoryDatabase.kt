package com.anb.git.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LocalRepository(
    val id: Long,
    val name: String,
    val folderName: String,
    val createdAt: Long
)

class RepositoryDatabase(context: Context) :
    SQLiteOpenHelper(context, "anb_git.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE repositories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                "folder_name TEXT NOT NULL UNIQUE, " +
                "created_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add explicit migrations here when the schema version changes.
    }

    fun getAll(): List<LocalRepository> {
        val result = mutableListOf<LocalRepository>()
        readableDatabase.query(
            "repositories",
            arrayOf("id", "name", "folder_name", "created_at"),
            null, null, null, null,
            "name COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += LocalRepository(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getLong(3)
                )
            }
        }
        return result
    }

    fun insert(name: String, folderName: String, createdAt: Long) {
        val values = ContentValues().apply {
            put("name", name)
            put("folder_name", folderName)
            put("created_at", createdAt)
        }
        writableDatabase.insertOrThrow("repositories", null, values)
    }

    fun rename(id: Long, name: String, folderName: String) {
        val values = ContentValues().apply {
            put("name", name)
            put("folder_name", folderName)
        }
        val updated = writableDatabase.update(
            "repositories", values, "id = ?", arrayOf(id.toString())
        )
        check(updated == 1) { "Repository record was not found." }
    }

    fun delete(id: Long) {
        writableDatabase.delete("repositories", "id = ?", arrayOf(id.toString()))
    }
}
