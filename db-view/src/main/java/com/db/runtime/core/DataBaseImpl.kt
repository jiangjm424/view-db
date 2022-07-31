/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */
package com.db.runtime.core

import android.net.Uri
import android.util.Pair
import androidx.sqlite.db.SupportSQLiteDatabase
import com.db.runtime.model.Response
import com.db.runtime.model.RowDataRequest
import com.db.runtime.model.TableDataResponse
import com.db.runtime.model.UpdateRowResponse
import com.db.runtime.sqlite.DBFactory
import com.db.runtime.sqlite.InMemoryDebugSQLiteDB
import com.db.runtime.sqlite.SQLiteDB
import com.db.runtime.utils.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.*
import java.net.URLDecoder
import java.util.*

/**
 * Created by amitshekhar on 06/02/17.
 */
class DataBaseImpl(private val mDbFactory: DBFactory) {
    private val mContext get() = DBViewCore.context
    private val mGson: Gson = GsonBuilder().serializeNulls().create()
    private var isDbOpened = false
    private var sqLiteDB: SQLiteDB? = null
    private var mDatabaseFiles: HashMap<String, Pair<File, String>>? = null
    private var mCustomDatabaseFiles: HashMap<String, Pair<File, String>>? = null
    var mSelectedDatabase: String? = null
        private set
    private var mRoomInMemoryDatabases: HashMap<String, SupportSQLiteDatabase>? =
        HashMap<String, SupportSQLiteDatabase>()

    fun setCustomDatabaseFiles(customDatabaseFiles: HashMap<String, Pair<File, String>>?) {
        mCustomDatabaseFiles = customDatabaseFiles
    }

    fun setInMemoryRoomDatabases(databases: HashMap<String, SupportSQLiteDatabase>?) {
        mRoomInMemoryDatabases = databases
    }

    fun writeServerError(output: PrintStream) {
        output.println("HTTP/1.0 500 Internal Server Error")
        output.flush()
    }

    fun openDatabase(database: String?) {
        closeDatabase()
        if (mRoomInMemoryDatabases!!.containsKey(database)) {
            sqLiteDB = mRoomInMemoryDatabases!![database]?.let { InMemoryDebugSQLiteDB(it) }
        } else {
            val databaseFile = mDatabaseFiles!![database]!!.first
            val password = mDatabaseFiles!![database]!!.second
            sqLiteDB = mDbFactory.create(mContext, databaseFile.absolutePath, password)
        }
        isDbOpened = true
    }

    fun closeDatabase() {
        if (sqLiteDB != null && sqLiteDB!!.isOpen) {
            sqLiteDB!!.close()
        }
        sqLiteDB = null
        isDbOpened = false
    }


    fun getDBListResponse(): Response {
        mDatabaseFiles = DatabaseFileProvider.getDatabaseFiles(mContext)
        if (mCustomDatabaseFiles != null) {
            mDatabaseFiles!!.putAll(mCustomDatabaseFiles!!)
        }
        val response = Response()
        if (mDatabaseFiles != null) {
            for ((key, value) in mDatabaseFiles!!) {
                val dbEntry = arrayOf(key, if (value.second != "") "true" else "false", "true")
                response.rows.add(dbEntry)
            }
        }
        if (mRoomInMemoryDatabases != null) {
            for ((key) in mRoomInMemoryDatabases!!) {
                val dbEntry = arrayOf(key, "false", "false")
                response.rows.add(dbEntry)
            }
        }
        response.rows.add(arrayOf(Constants.APP_SHARED_PREFERENCES, "false", "false"))
        response.isSuccessful = true
        return response
    }

    //获取表数据
    //route = getAllDataFromTheTable?tableName=users
    //size -1 表示获取表中所有的数据，否则>0获取表中前 size条数据（有的话）
    fun getAllDataFromTheTableResponse(route: String, size: Int = -1): TableDataResponse {
        var tableName: String? = null
        if (route.contains("?tableName=")) {
            tableName = route.substring(route.indexOf("=") + 1, route.length)
        }
        val response: TableDataResponse = if (isDbOpened) {
            val sql = "SELECT * FROM $tableName"
            DatabaseHelper.getTableData(sqLiteDB!!, sql, tableName, size)
        } else {
            PrefHelper.getAllPrefData(mContext, tableName)
        }
        return response
    }

    fun executeQueryAndGetResponse(route: String): TableDataResponse? {
        var query: String? = null
        var data: TableDataResponse? = null
        var first: String
        try {
            if (route.contains("?query=")) {
                query = route.substring(route.indexOf("=") + 1, route.length)
            }
            try {
                query = URLDecoder.decode(query, "UTF-8")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (query != null) {
                val statements = query.split(";").toTypedArray()
                for (i in statements.indices) {
                    val aQuery = statements[i].trim { it <= ' ' }
                    first = aQuery.split(" ").toTypedArray()[0].lowercase(Locale.getDefault())
                    if (first == "select" || first == "pragma") {
                        val response: TableDataResponse =
                            DatabaseHelper.getTableData(sqLiteDB!!, aQuery, null)
                        data = response
                        if (!response.isSuccessful) {
                            break
                        }
                    } else {
                        val response: TableDataResponse = DatabaseHelper.exec(sqLiteDB!!, aQuery)
                        data = response
                        if (!response.isSuccessful) {
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    //获取数据库中的表
    //route = getTableList?database=User.db
    fun getTableListResponse(route: String): Response {
        var database: String? = null
        if (route.contains("?database=")) {
            database = route.substring(route.indexOf("=") + 1, route.length)
        }
        val response: Response
        if (Constants.APP_SHARED_PREFERENCES == database) {
            response = PrefHelper.getAllPrefTableName(mContext)
            closeDatabase()
            mSelectedDatabase = Constants.APP_SHARED_PREFERENCES
        } else {
            openDatabase(database)
            response = DatabaseHelper.getAllTableName(sqLiteDB!!)
            mSelectedDatabase = database
        }
        return response
    }

    //route = getAllDataFromTheTable?tableName=users
    fun getTableColumns(route: String): List<TableDataResponse.TableInfo>? {
        var tableName: String? = null
        if (route.contains("?tableName=")) {
            tableName = route.substring(route.indexOf("=") + 1, route.length)
        }
        val response = if (isDbOpened) {
            val sql = "SELECT * FROM $tableName"
            DatabaseHelper.getTableColumn(sqLiteDB!!, sql, tableName)
        } else {
            PrefHelper.getPrefColumn(mContext, tableName)
        }
        return response
    }

    fun addTableDataAndGetResponse(route: String): UpdateRowResponse {
        var response: UpdateRowResponse
        return try {
            val uri = Uri.parse(URLDecoder.decode(route, "UTF-8"))
            val tableName = uri.getQueryParameter("tableName")
            val updatedData = uri.getQueryParameter("addData")
            val rowDataRequests: List<RowDataRequest> = mGson.fromJson(
                updatedData,
                object : TypeToken<List<RowDataRequest>>() {}.type
            )
            response = if (Constants.APP_SHARED_PREFERENCES == mSelectedDatabase) {
                PrefHelper.addOrUpdateRow(mContext, tableName, rowDataRequests)
            } else {
                DatabaseHelper.addRow(sqLiteDB!!, tableName, rowDataRequests)
            }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            response = UpdateRowResponse()
            response.isSuccessful = false
            response
        }
    }

    fun updateTableDataAndGetResponse(route: String): UpdateRowResponse {
        var response: UpdateRowResponse
        return try {
            val uri = Uri.parse(URLDecoder.decode(route, "UTF-8"))
            val tableName = uri.getQueryParameter("tableName")
            val updatedData = uri.getQueryParameter("updatedData")
            val rowDataRequests: List<RowDataRequest> = mGson.fromJson<List<RowDataRequest>>(
                updatedData,
                object : TypeToken<List<RowDataRequest?>?>() {}.type
            )
            response = if (Constants.APP_SHARED_PREFERENCES == mSelectedDatabase) {
                PrefHelper.addOrUpdateRow(mContext, tableName, rowDataRequests)
            } else {
                DatabaseHelper.updateRow(sqLiteDB!!, tableName, rowDataRequests)
            }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            response = UpdateRowResponse()
            response.isSuccessful = false
            response
        }
    }

    fun deleteTableDataAndGetResponse(route: String): UpdateRowResponse {
        var response: UpdateRowResponse
        return try {
            val uri = Uri.parse(URLDecoder.decode(route, "UTF-8"))
            val tableName = uri.getQueryParameter("tableName")
            val updatedData = uri.getQueryParameter("deleteData")
            val rowDataRequests: List<RowDataRequest> = mGson.fromJson<List<RowDataRequest>>(
                updatedData,
                object : TypeToken<List<RowDataRequest?>?>() {}.type
            )
            response = if (Constants.APP_SHARED_PREFERENCES == mSelectedDatabase) {
                PrefHelper.deleteRow(mContext, tableName, rowDataRequests)
            } else {
                DatabaseHelper.deleteRow(sqLiteDB!!, tableName, rowDataRequests)
            }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            response = UpdateRowResponse()
            response.isSuccessful = false
            response
        }
    }

    fun deleteSelectedDatabaseAndGetResponse(): UpdateRowResponse {
        val response = UpdateRowResponse()
        if (mSelectedDatabase == null || !mDatabaseFiles!!.containsKey(mSelectedDatabase)) {
            response.isSuccessful = false
            return response
        }
        return try {
            closeDatabase()
            val dbFile = mDatabaseFiles!![mSelectedDatabase]!!.first
            response.isSuccessful = dbFile.delete()
            if (response.isSuccessful) {
                mDatabaseFiles!!.remove(mSelectedDatabase)
                mCustomDatabaseFiles!!.remove(mSelectedDatabase)
            }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            response.isSuccessful = false
            response
        }
    }

    fun downloadDb() = Utils.getDatabase(mSelectedDatabase, mDatabaseFiles)
}
