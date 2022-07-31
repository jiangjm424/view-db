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
package com.db.runtime.utils

import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import com.db.runtime.model.Response
import com.db.runtime.sqlite.SQLiteDB
import com.db.runtime.model.RowDataRequest
import com.db.runtime.model.TableDataResponse
import com.db.runtime.model.TableDataResponse.ColumnData
import com.db.runtime.model.UpdateRowResponse

/**
 * Created by amitshekhar on 06/02/17.
 */
object DatabaseHelper {
    fun getAllTableName(database: SQLiteDB): Response {
        val response = Response()
        val c = database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' OR type='view' ORDER BY name COLLATE NOCASE",
            null
        )
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                response.rows.add(c.getString(0))
                c.moveToNext()
            }
        }
        c.close()
        response.isSuccessful = true
        try {
            response.dbVersion = database.version
        } catch (ignore: Exception) {
        }
        return response
    }


    fun getTableColumn(
        db: SQLiteDB,
        selectQuery: String,
        tableName: String?
    ): MutableList<TableDataResponse.TableInfo>? {
        var selectQuery = selectQuery
        var tableName = tableName
        val tableData = TableDataResponse()
        tableData.isSelectQuery = true
        if (tableName == null) {
            tableName = getTableName(selectQuery)
        }
        val quotedTableName = getQuotedTableName(tableName)
        if (tableName != null) {
            val pragmaQuery = "PRAGMA table_info($quotedTableName)"
            tableData.tableInfos = getTableInfo(db, pragmaQuery)
        }
        if (tableData.tableInfos != null) return tableData.tableInfos
        var cursor: Cursor? = null
        var isView = false
        try {
            cursor =
                db.rawQuery("SELECT type FROM sqlite_master WHERE name=?", arrayOf(quotedTableName))
            if (cursor.moveToFirst()) {
                isView = "view".equals(cursor.getString(0), ignoreCase = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        tableData.isEditable = tableName != null && tableData.tableInfos != null && !isView
        if (!TextUtils.isEmpty(tableName)) {
            selectQuery = selectQuery.replace(tableName!!, quotedTableName)
        }
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: Exception) {
            e.printStackTrace()
            tableData.isSuccessful = false
            tableData.errorMessage = e.message
            return tableData.tableInfos
        }
        return run {
            cursor.moveToFirst()

            // setting tableInfo when tableName is not known and making
            // it non-editable also by making isPrimary true for all
            if (tableData.tableInfos == null) {
                tableData.tableInfos = ArrayList()
                for (i in 0 until cursor.columnCount) {
                    val tableInfo = TableDataResponse.TableInfo()
                    tableInfo.title = cursor.getColumnName(i)
                    tableInfo.isPrimary = true
                    tableData.tableInfos?.add(tableInfo)
                }
            }
            tableData.isSuccessful = true
            tableData.rows = ArrayList()
            val columnNames = cursor.columnNames
            val tableInfoListModified: MutableList<TableDataResponse.TableInfo> = ArrayList()
            for (columnName in columnNames) {
                for (tableInfo in tableData.tableInfos!!) {
                    if (columnName == tableInfo!!.title) {
                        tableInfoListModified.add(tableInfo)
                        break
                    }
                }
            }
            if (tableData.tableInfos!!.size != tableInfoListModified.size) {
                tableData.tableInfos = tableInfoListModified
                tableData.isEditable = false
            }
            cursor.close()
            tableData.tableInfos
        }
    }

    fun getTableData(
        db: SQLiteDB,
        selectQuery: String,
        tableName: String?,
        size: Int = -1
    ): TableDataResponse {
        var selectQuery = selectQuery
        var tableName = tableName
        val tableData = TableDataResponse()
        tableData.isSelectQuery = true
        if (tableName == null) {
            tableName = getTableName(selectQuery)
        }
        val quotedTableName = getQuotedTableName(tableName)
        if (tableName != null) {
            val pragmaQuery = "PRAGMA table_info($quotedTableName)"
            tableData.tableInfos = getTableInfo(db, pragmaQuery)
        }
        var cursor: Cursor? = null
        var isView = false
        try {
            cursor =
                db.rawQuery("SELECT type FROM sqlite_master WHERE name=?", arrayOf(quotedTableName))
            if (cursor.moveToFirst()) {
                isView = "view".equals(cursor.getString(0), ignoreCase = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        tableData.isEditable = tableName != null && tableData.tableInfos != null && !isView
        if (!TextUtils.isEmpty(tableName)) {
            selectQuery = selectQuery.replace(tableName!!, quotedTableName)
        }
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: Exception) {
            e.printStackTrace()
            tableData.isSuccessful = false
            tableData.errorMessage = e.message
            return tableData
        }
        cursor.moveToFirst()

        // setting tableInfo when tableName is not known and making
        // it non-editable also by making isPrimary true for all
        if (tableData.tableInfos == null) {
            tableData.tableInfos = ArrayList()
            for (i in 0 until cursor.columnCount) {
                val tableInfo = TableDataResponse.TableInfo()
                tableInfo.title = cursor.getColumnName(i)
                tableInfo.isPrimary = true
                tableData.tableInfos?.add(tableInfo)
            }
        }
        tableData.isSuccessful = true
        tableData.rows = ArrayList()
        val columnNames = cursor.columnNames
        val tableInfoListModified: MutableList<TableDataResponse.TableInfo> = ArrayList()
        for (columnName in columnNames) {
            for (tableInfo in tableData.tableInfos!!) {
                if (columnName == tableInfo!!.title) {
                    tableInfoListModified.add(tableInfo)
                    break
                }
            }
        }
        if (tableData.tableInfos!!.size != tableInfoListModified.size) {
            tableData.tableInfos = tableInfoListModified
            tableData.isEditable = false
        }
        var pp = size
        if (cursor.count > 0) {
            do {
                val row: MutableList<ColumnData> = ArrayList()
                for (i in 0 until cursor.columnCount) {
                    val columnData = ColumnData()
                    when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_BLOB -> {
                            columnData.dataType = DataType.TEXT
                            columnData.value = ConverterUtils.blobToString(cursor.getBlob(i))
                        }
                        Cursor.FIELD_TYPE_FLOAT -> {
                            columnData.dataType = DataType.REAL
                            columnData.value = cursor.getDouble(i)
                        }
                        Cursor.FIELD_TYPE_INTEGER -> {
                            columnData.dataType = DataType.INTEGER
                            columnData.value = cursor.getLong(i)
                        }
                        Cursor.FIELD_TYPE_STRING -> {
                            columnData.dataType = DataType.TEXT
                            columnData.value = cursor.getString(i)
                        }
                        else -> {
                            columnData.dataType = DataType.TEXT
                            columnData.value = cursor.getString(i)
                        }
                    }
                    row.add(columnData)
                }
                tableData.rows?.add(row)
                pp--
                if (size > 0 && pp == 0) {
                    break
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return tableData
    }

    private fun getQuotedTableName(tableName: String?): String {
        return String.format("[%s]", tableName)
    }

    private fun getTableInfo(
        db: SQLiteDB,
        pragmaQuery: String
    ): MutableList<TableDataResponse.TableInfo>? {
        val cursor: Cursor = try {
            db.rawQuery(pragmaQuery, null)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        val tableInfoList: MutableList<TableDataResponse.TableInfo> = ArrayList()
        cursor.moveToFirst()
        if (cursor.count > 0) {
            do {
                val tableInfo = TableDataResponse.TableInfo()
                for (i in 0 until cursor.columnCount) {
                    when (cursor.getColumnName(i)) {
                        Constants.PK -> tableInfo.isPrimary = cursor.getInt(i) == 1
                        Constants.NAME -> tableInfo.title = cursor.getString(i)
                        else -> {}
                    }
                }
                tableInfoList.add(tableInfo)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return tableInfoList
    }

    fun addRow(
        db: SQLiteDB, tableName: String?,
        rowDataRequests: List<RowDataRequest>?
    ): UpdateRowResponse {
        var tableName = tableName
        val updateRowResponse = UpdateRowResponse()
        if (rowDataRequests == null || tableName == null) {
            updateRowResponse.isSuccessful = false
            return updateRowResponse
        }
        tableName = getQuotedTableName(tableName)
        val contentValues = ContentValues()
        for (rowDataRequest in rowDataRequests) {
            if (Constants.NULL == rowDataRequest.value) {
                rowDataRequest.value = null
            }
            when (rowDataRequest.dataType) {
                DataType.INTEGER -> contentValues.put(
                    rowDataRequest.title,
                    java.lang.Long.valueOf(rowDataRequest.value)
                )
                DataType.REAL -> contentValues.put(
                    rowDataRequest.title,
                    java.lang.Double.valueOf(rowDataRequest.value)
                )
                DataType.TEXT -> contentValues.put(rowDataRequest.title, rowDataRequest.value)
                else -> contentValues.put(rowDataRequest.title, rowDataRequest.value)
            }
        }
        val result = db.insert(tableName, null, contentValues)
        updateRowResponse.isSuccessful = result > 0
        return updateRowResponse
    }

    fun updateRow(
        db: SQLiteDB,
        tableName: String?,
        rowDataRequests: List<RowDataRequest>?
    ): UpdateRowResponse {
        var tableName = tableName
        val updateRowResponse = UpdateRowResponse()
        if (rowDataRequests == null || tableName == null) {
            updateRowResponse.isSuccessful = false
            return updateRowResponse
        }
        tableName = getQuotedTableName(tableName)
        val contentValues = ContentValues()
        var whereClause: String? = null
        val whereArgsList: MutableList<String?> = ArrayList()
        for (rowDataRequest in rowDataRequests) {
            if (Constants.NULL == rowDataRequest.value) {
                rowDataRequest.value = null
            }
            if (rowDataRequest.isPrimary) {
                whereClause = if (whereClause == null) {
                    rowDataRequest.title + "=? "
                } else {
                    whereClause + "and " + rowDataRequest.title + "=? "
                }
                whereArgsList.add(rowDataRequest.value)
            } else {
                when (rowDataRequest.dataType) {
                    DataType.INTEGER -> contentValues.put(
                        rowDataRequest.title,
                        java.lang.Long.valueOf(rowDataRequest.value)
                    )
                    DataType.REAL -> contentValues.put(
                        rowDataRequest.title,
                        java.lang.Double.valueOf(rowDataRequest.value)
                    )
                    DataType.TEXT -> contentValues.put(rowDataRequest.title, rowDataRequest.value)
                    else -> {}
                }
            }
        }
        val whereArgs = arrayOfNulls<String>(whereArgsList.size)
        for (i in whereArgsList.indices) {
            whereArgs[i] = whereArgsList[i]
        }
        db.update(tableName, contentValues, whereClause, whereArgs)
        updateRowResponse.isSuccessful = true
        return updateRowResponse
    }

    fun deleteRow(
        db: SQLiteDB, tableName: String?,
        rowDataRequests: List<RowDataRequest>?
    ): UpdateRowResponse {
        var tableName = tableName
        val updateRowResponse = UpdateRowResponse()
        if (rowDataRequests == null || tableName == null) {
            updateRowResponse.isSuccessful = false
            return updateRowResponse
        }
        tableName = getQuotedTableName(tableName)
        var whereClause: String? = null
        val whereArgsList: MutableList<String?> = ArrayList()
        for (rowDataRequest in rowDataRequests) {
            if (Constants.NULL == rowDataRequest.value) {
                rowDataRequest.value = null
            }
            if (rowDataRequest.isPrimary) {
                whereClause = if (whereClause == null) {
                    rowDataRequest.title + "=? "
                } else {
                    whereClause + "and " + rowDataRequest.title + "=? "
                }
                whereArgsList.add(rowDataRequest.value)
            }
        }
        if (whereArgsList.size == 0) {
            updateRowResponse.isSuccessful = true
            return updateRowResponse
        }
        val whereArgs = arrayOfNulls<String>(whereArgsList.size)
        for (i in whereArgsList.indices) {
            whereArgs[i] = whereArgsList[i]
        }
        db.delete(tableName, whereClause, whereArgs)
        updateRowResponse.isSuccessful = true
        return updateRowResponse
    }

    fun exec(database: SQLiteDB, sql: String): TableDataResponse {
        var sql = sql
        val tableDataResponse = TableDataResponse()
        tableDataResponse.isSelectQuery = false
        try {
            val tableName = getTableName(sql)
            if (!TextUtils.isEmpty(tableName)) {
                val quotedTableName = getQuotedTableName(tableName)
                sql = sql.replace(tableName!!, quotedTableName)
            }
            database.execSQL(sql)
        } catch (e: Exception) {
            e.printStackTrace()
            tableDataResponse.isSuccessful = false
            tableDataResponse.errorMessage = e.message
            return tableDataResponse
        }
        tableDataResponse.isSuccessful = true
        return tableDataResponse
    }

    private fun getTableName(selectQuery: String): String? {
        // TODO: Handle JOIN Query
        val tableNameParser = TableNameParser(selectQuery)
        val tableNames = tableNameParser.tables() as HashSet<String?>
        for (tableName in tableNames) {
            if (!TextUtils.isEmpty(tableName)) {
                return tableName
            }
        }
        return null
    }
}
