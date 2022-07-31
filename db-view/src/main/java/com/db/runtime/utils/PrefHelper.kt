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

import android.content.Context
import com.db.runtime.model.Response
import com.db.runtime.model.RowDataRequest
import com.db.runtime.model.TableDataResponse
import com.db.runtime.model.TableDataResponse.ColumnData
import com.db.runtime.model.UpdateRowResponse
import org.json.JSONArray
import java.io.File
import java.util.*

/**
 * Created by amitshekhar on 06/02/17.
 */
object PrefHelper {
    private const val PREFS_SUFFIX = ".xml"
    fun getSharedPreferenceTags(context: Context): List<String> {
        val tags = ArrayList<String>()
        val rootPath = context.applicationInfo.dataDir + "/shared_prefs"
        val root = File(rootPath)
        if (root.exists()) {
            for (file in root.listFiles()) {
                val fileName = file.name
                if (fileName.endsWith(PREFS_SUFFIX)) {
                    tags.add(fileName.substring(0, fileName.length - PREFS_SUFFIX.length))
                }
            }
        }
        Collections.sort(tags)
        return tags
    }

    fun getPrefColumn(context: Context, tag: String?): MutableList<TableDataResponse.TableInfo>? {
        val response = TableDataResponse()
        response.isEditable = true
        response.isSuccessful = true
        response.isSelectQuery = true
        val keyInfo = TableDataResponse.TableInfo()
        keyInfo.isPrimary = true
        keyInfo.title = "Key"
        val valueInfo = TableDataResponse.TableInfo()
        valueInfo.isPrimary = false
        valueInfo.title = "Value"
        response.tableInfos = ArrayList()
        response.tableInfos?.add(keyInfo)
        response.tableInfos?.add(valueInfo)
        return response.tableInfos
    }

    fun getAllPrefTableName(context: Context): Response {
        val response = Response()
        val prefTags = getSharedPreferenceTags(context)
        for (tag in prefTags) {
            response.rows.add(tag)
        }
        response.isSuccessful = true
        return response
    }

    fun getAllPrefData(context: Context, tag: String?): TableDataResponse {
        val response = TableDataResponse()
        response.isEditable = true
        response.isSuccessful = true
        response.isSelectQuery = true
        val keyInfo = TableDataResponse.TableInfo()
        keyInfo.isPrimary = true
        keyInfo.title = "Key"
        val valueInfo = TableDataResponse.TableInfo()
        valueInfo.isPrimary = false
        valueInfo.title = "Value"
        response.tableInfos = ArrayList()
        response.tableInfos?.add(keyInfo)
        response.tableInfos?.add(valueInfo)
        response.rows = ArrayList()
        val preferences = context.getSharedPreferences(tag, Context.MODE_PRIVATE)
        val allEntries = preferences.all
        for ((key, value) in allEntries) {
            val row: MutableList<ColumnData> = ArrayList()
            val keyColumnData = ColumnData()
            keyColumnData.dataType = DataType.TEXT
            keyColumnData.value = key
            row.add(keyColumnData)
            val valueColumnData = ColumnData()
            valueColumnData.value = value.toString()
            if (value != null) {
                if (value is String) {
                    valueColumnData.dataType = DataType.TEXT
                } else if (value is Int) {
                    valueColumnData.dataType = DataType.INTEGER
                } else if (value is Long) {
                    valueColumnData.dataType = DataType.LONG
                } else if (value is Float) {
                    valueColumnData.dataType = DataType.FLOAT
                } else if (value is Boolean) {
                    valueColumnData.dataType = DataType.BOOLEAN
                } else if (value is Set<*>) {
                    valueColumnData.dataType = DataType.STRING_SET
                }
            } else {
                valueColumnData.dataType = DataType.TEXT
            }
            row.add(valueColumnData)
            response.rows?.add(row)
        }
        return response
    }

    fun addOrUpdateRow(
        context: Context, tableName: String?,
        rowDataRequests: List<RowDataRequest>
    ): UpdateRowResponse {
        val updateRowResponse = UpdateRowResponse()
        if (tableName == null) {
            return updateRowResponse
        }
        val rowDataKey = rowDataRequests[0]
        val rowDataValue = rowDataRequests[1]
        val key = rowDataKey.value
        var value = rowDataValue.value
        val dataType = rowDataValue.dataType
        if (Constants.NULL == value) {
            value = null
        }
        val preferences = context.getSharedPreferences(tableName, Context.MODE_PRIVATE)
        try {
            when (dataType) {
                DataType.TEXT -> {
                    preferences.edit().putString(key, value).apply()
                    updateRowResponse.isSuccessful = true
                }
                DataType.INTEGER -> {
                    preferences.edit().putInt(key, Integer.valueOf(value)).apply()
                    updateRowResponse.isSuccessful = true
                }
                DataType.LONG -> {
                    preferences.edit().putLong(key, java.lang.Long.valueOf(value)).apply()
                    updateRowResponse.isSuccessful = true
                }
                DataType.FLOAT -> {
                    preferences.edit().putFloat(key, java.lang.Float.valueOf(value)).apply()
                    updateRowResponse.isSuccessful = true
                }
                DataType.BOOLEAN -> {
                    preferences.edit().putBoolean(key, java.lang.Boolean.valueOf(value)).apply()
                    updateRowResponse.isSuccessful = true
                }
                DataType.STRING_SET -> {
                    val jsonArray = JSONArray(value)
                    val stringSet: MutableSet<String> = HashSet()
                    var i = 0
                    while (i < jsonArray.length()) {
                        stringSet.add(jsonArray.getString(i))
                        i++
                    }
                    preferences.edit().putStringSet(key, stringSet).apply()
                    updateRowResponse.isSuccessful = true
                }
                else -> {
                    preferences.edit().putString(key, value).apply()
                    updateRowResponse.isSuccessful = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return updateRowResponse
    }

    fun deleteRow(
        context: Context, tableName: String?,
        rowDataRequests: List<RowDataRequest>
    ): UpdateRowResponse {
        val updateRowResponse = UpdateRowResponse()
        if (tableName == null) {
            return updateRowResponse
        }
        val rowDataKey = rowDataRequests[0]
        val key = rowDataKey.value
        val preferences = context.getSharedPreferences(tableName, Context.MODE_PRIVATE)
        try {
            preferences.edit()
                .remove(key).apply()
            updateRowResponse.isSuccessful = true
        } catch (ex: Exception) {
            updateRowResponse.isSuccessful = false
        }
        return updateRowResponse
    }
}
