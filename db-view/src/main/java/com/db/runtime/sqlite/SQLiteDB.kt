package com.db.runtime.sqlite

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException

/**
 * Created by anandgaurav on 12/02/18.
 */
interface SQLiteDB {
    fun delete(table: String?, whereClause: String?, whereArgs: Array<String?>?): Int
    val isOpen: Boolean
    fun close()
    fun rawQuery(sql: String?, selectionArgs: Array<String>?): Cursor

    @Throws(SQLException::class)
    fun execSQL(sql: String?)
    fun insert(table: String?, nullColumnHack: String?, values: ContentValues?): Long
    fun update(
        table: String?,
        values: ContentValues?,
        whereClause: String?,
        whereArgs: Array<String?>?
    ): Int

    val version: Int
}
