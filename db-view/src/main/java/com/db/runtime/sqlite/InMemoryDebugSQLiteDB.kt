package com.db.runtime.sqlite

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Created by anandgaurav on 12/02/18.
 */
class InMemoryDebugSQLiteDB(database: SupportSQLiteDatabase) : SQLiteDB {
    private val database: SupportSQLiteDatabase
    override fun delete(table: String?, whereClause: String?, whereArgs: Array<String?>?): Int {
        return database.delete(table, whereClause, whereArgs)
    }

    override val isOpen: Boolean
        get() = database.isOpen

    override fun close() {
        // no ops
    }

    override fun rawQuery(sql: String?, selectionArgs: Array<String>?): Cursor {
        return database.query(sql, selectionArgs)
    }

    @Throws(SQLException::class)
    override fun execSQL(sql: String?) {
        database.execSQL(sql)
    }

    override fun insert(table: String?, nullColumnHack: String?, values: ContentValues?): Long {
        return database.insert(table, 0, values)
    }

    override fun update(
        table: String?,
        values: ContentValues?,
        whereClause: String?,
        whereArgs: Array<String?>?
    ): Int {
        return database.update(table, 0, values, whereClause, whereArgs)
    }

    override val version: Int
        get() = database.version

    init {
        this.database = database
    }
}
