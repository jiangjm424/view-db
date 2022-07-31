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

import android.content.res.AssetManager
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import java.io.*
import java.lang.Exception
import java.util.HashMap

/**
 * Created by amitshekhar on 06/02/17.
 */
object Utils {
    private const val TAG = "Utils"
    fun detectMimeType(fileName: String): String? {
        return if (TextUtils.isEmpty(fileName)) {
            null
        } else if (fileName.endsWith(".html")) {
            "text/html"
        } else if (fileName.endsWith(".js")) {
            "application/javascript"
        } else if (fileName.endsWith(".css")) {
            "text/css"
        } else {
            "application/octet-stream"
        }
    }

    @Throws(IOException::class)
    fun loadContent(fileName: String?, assetManager: AssetManager): ByteArray? {
        var input: InputStream? = null
        return try {
            val output = ByteArrayOutputStream()
            input = assetManager.open(fileName!!)
            val buffer = ByteArray(1024)
            var size: Int
            while (-1 != input.read(buffer).also { size = it }) {
                output.write(buffer, 0, size)
            }
            output.flush()
            output.toByteArray()
        } catch (e: FileNotFoundException) {
            null
        } finally {
            try {
                input?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getDatabase(
        selectedDatabase: String?,
        databaseFiles: HashMap<String, Pair<File, String>>?
    ): ByteArray? {
        if (TextUtils.isEmpty(selectedDatabase) || !databaseFiles!!.containsKey(selectedDatabase)) {
            return null
        }
        var byteArray: ByteArray? = ByteArray(0)
        try {
            val file = databaseFiles[selectedDatabase]!!.first
            byteArray = null
            try {
                val inputStream: InputStream = FileInputStream(file)
                val bos = ByteArrayOutputStream()
                val b = ByteArray(file.length().toInt())
                var bytesRead: Int
                while (inputStream.read(b).also { bytesRead = it } != -1) {
                    bos.write(b, 0, bytesRead)
                }
                byteArray = bos.toByteArray()
            } catch (e: IOException) {
                Log.e(TAG, "getDatabase: ", e)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return byteArray
    }
}
