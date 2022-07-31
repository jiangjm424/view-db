package com.db.runtime.sqliteimpl

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.db.runtime.sqlite.DBFactory
import com.db.runtime.sqlite.SQLiteDB
import com.db.runtime.utils.Constants
import net.sqlcipher.database.SQLiteDatabase as SafeSQLiteDatabase

class DBFactoryImpl : DBFactory {
    override fun create(context: Context, path: String, password: String?): SQLiteDB {
        Log.i(Constants.TAG_DEBUG, "create db with: $context, $path, $password")
        return password?.takeIf { it.isNotEmpty() }?.let {
            SafeSQLiteDatabase.loadLibs(context)
            return ViewSafeSqliteDB(SafeSQLiteDatabase.openOrCreateDatabase(path, it, null))
        } ?: ViewSqliteDB(SQLiteDatabase.openOrCreateDatabase(path, null))
    }
}
