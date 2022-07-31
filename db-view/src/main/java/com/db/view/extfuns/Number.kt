/*
 * Copyright (C)  guolin, Glance Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.db.view.extfuns

import com.db.runtime.core.DBViewCore
import java.lang.StringBuilder

/**
 * Number extension methods.
 *
 * @author guolin
 * @since 2020/9/24
 */

/**
 * Convert dp to px.
 */
val Int.dp: Int
    get() {
        val scale = DBViewCore.context.resources.displayMetrics.density
        return (this * scale + 0.5).toInt()
    }

val Float.dp: Float
    get() {
        val scale = DBViewCore.context.resources.displayMetrics.density
        return (this * scale + 0.5).toFloat()
    }

val Double.dp: Double
    get() {
        val scale = DBViewCore.context.resources.displayMetrics.density
        return this * scale + 0.5
    }

/**
 * Convert a number to a numeric string.
 * e.g. 12365 wil be converted into 12,365
 */
fun Int.toNumericString(): String {
    val chars = toString().toCharArray()
    chars.reverse()
    val builder = StringBuilder()
    chars.forEachIndexed { index, c ->
        if (index != 0 && index % 3 == 0 && c != '-') {
            builder.append(",")
        }
        builder.append(c)
    }
    return builder.reverse().toString()
}
