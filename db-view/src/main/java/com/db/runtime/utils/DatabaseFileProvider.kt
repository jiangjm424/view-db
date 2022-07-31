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
import android.content.SharedPreferences
import android.util.Pair
import java.io.File
import java.text.MessageFormat
import java.util.*

/**
 * Created by amitshekhar on 06/02/17.
 */
object DatabaseFileProvider {
    fun getDatabaseFiles(context: Context): HashMap<String, Pair<File, String>> {
        val databaseFiles = HashMap<String, Pair<File, String>>()
        try {
            val pref = context.getSharedPreferences("db_viewer", Context.MODE_PRIVATE)
            for (databaseName in context.databaseList()) {
                val password = getDbPasswordFromStringResources(databaseName, pref)
                databaseFiles[databaseName] = Pair(context.getDatabasePath(databaseName), password)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return databaseFiles
    }

    private fun getDbPasswordFromStringResources(
        name: String,
        pref: SharedPreferences
    ): String {
        return pref.getString(name, "") ?: ""
    }
}
