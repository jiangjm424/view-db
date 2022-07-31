package com.db.runtime.sqliteimpl

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.db.runtime.sqlite.SQLiteDB

class ViewSqliteDB(private val sqliteDatabase: SQLiteDatabase) : SQLiteDB {
    override fun delete(table: String?, whereClause: String?, whereArgs: Array<String?>?): Int {
        return sqliteDatabase.delete(table, whereClause, whereArgs)
    }

    override val isOpen: Boolean
        get() = sqliteDatabase.isOpen

    override fun close() {
        sqliteDatabase.close()
    }

    override fun rawQuery(sql: String?, selectionArgs: Array<String>?): Cursor {
        return sqliteDatabase.rawQuery(sql, selectionArgs)
    }

    override fun execSQL(sql: String?) {
        sqliteDatabase.execSQL(sql)
    }

    override fun insert(table: String?, nullColumnHack: String?, values: ContentValues?): Long {
        return sqliteDatabase.insert(table, nullColumnHack, values)
    }

    override fun update(
        table: String?,
        values: ContentValues?,
        whereClause: String?,
        whereArgs: Array<String?>?
    ): Int {
        return sqliteDatabase.update(table, values, whereClause, whereArgs)
    }

    override val version: Int
        get() = sqliteDatabase.version
}
