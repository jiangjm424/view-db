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
package com.db.runtime.server

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.db.runtime.core.DataBaseImpl
import com.db.runtime.utils.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.net.Socket

/**
 * Created by amitshekhar on 06/02/17.
 */
class RequestHandler(context: Context, private val dbHelper: DataBaseImpl) {
    private val mAssets = context.resources.assets
    private val mGson: Gson = GsonBuilder().serializeNulls().create()
    @Throws(IOException::class)
    fun handle(socket: Socket) {
        var reader: BufferedReader? = null
        var output: PrintStream? = null
        try {
            var route: String? = null

            // Read HTTP headers and parse out the route.
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String
            while (!TextUtils.isEmpty(reader.readLine().also { line = it })) {
                if (line.startsWith("GET /")) {
                    val start = line.indexOf('/') + 1
                    val end = line.indexOf(' ', start)
                    route = line.substring(start, end)
                    break
                }
            }

            // Output stream that we send the response to
            output = PrintStream(socket.getOutputStream())
            if (route == null || route.isEmpty()) {
                route = "index.html"
            }
            Log.i("jiang","route: $route")
            val bytes: ByteArray? = if (route.startsWith("getDbList")) {
                val response = dbHelper.getDBListResponse()
                mGson.toJson(response).toByteArray()
            } else if (route.startsWith("getAllDataFromTheTable")) {
                val response = dbHelper.getAllDataFromTheTableResponse(route)
                mGson.toJson(response).toByteArray()
            } else if (route.startsWith("getTableList")) {
                val response = dbHelper.getTableListResponse(route)
                mGson.toJson(response).toByteArray()
            } else if (route.startsWith("addTableData")) {
                val response = dbHelper.addTableDataAndGetResponse(route)
                mGson.toJson(response).toByteArray()
            } else if (route.startsWith("updateTableData")) {
                val response = dbHelper.updateTableDataAndGetResponse(route)
                mGson.toJson(response).toByteArray()
            } else if (route.startsWith("deleteTableData")) {
                val response = dbHelper.deleteTableDataAndGetResponse(route)
                mGson.toJson(response).toByteArray()
            } else if (route.startsWith("query")) {
                val response = dbHelper.executeQueryAndGetResponse(route)
                mGson.toJson(response)!!.toByteArray()
            } else if (route.startsWith("deleteDb")) {
                val response = dbHelper.deleteSelectedDatabaseAndGetResponse()
                mGson.toJson(response).toByteArray()
            } else if (route.startsWith("downloadDb")) {
                dbHelper.downloadDb()
            } else {
                Utils.loadContent(route, mAssets)
            }
            if (null == bytes) {
                writeServerError(output)
                return
            }

            // Send out the content.
            output.println("HTTP/1.0 200 OK")
            output.println("Content-Type: " + Utils.detectMimeType(route))
            if (route.startsWith("downloadDb")) {
                output.println("Content-Disposition: attachment; filename=${dbHelper.mSelectedDatabase}")
            } else {
                output.println("Content-Length: " + bytes.size)
            }
            output.println()
            output.write(bytes)
            output.flush()
        } finally {
            try {
                output?.close()
                reader?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun writeServerError(output: PrintStream) {
        output.println("HTTP/1.0 500 Internal Server Error")
        output.flush()
    }
}
