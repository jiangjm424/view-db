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

package com.db.view.page.db

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.db.view.R
import com.db.view.databinding.LayoutDbItemBinding
import com.db.view.extfuns.setExtraMarginForFirstAndLastItem
import com.db.view.extfuns.visible
import com.db.view.model.DBFile
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for the RecyclerView to show all the db files of current app.
 *
 * @author guolin
 * @since 2020/8/26
 */
class DBAdapter(
    private val dbList: List<DBFile>,
    private val click: ((Int, DBFile) -> Unit)? = null
) : RecyclerView.Adapter<DBAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "DBAdapter"
    }

    lateinit var context: Context

    class ViewHolder(dbItemBinding: LayoutDbItemBinding) :
        RecyclerView.ViewHolder(dbItemBinding.root) {
        val lock: ImageView = dbItemBinding.lock
        val dbNameText: TextView = dbItemBinding.dbNameText
        val dbPathText: TextView = dbItemBinding.dbPathText
        val modifyTimeText: TextView = dbItemBinding.modifyTimeText
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (!::context.isInitialized) context = parent.context
        val dbItemBinding = LayoutDbItemBinding.inflate(LayoutInflater.from(context), parent, false)
        val holder = ViewHolder(dbItemBinding)
        holder.itemView.setOnClickListener {
            Log.i(TAG, "item: click")
            val pos = holder.bindingAdapterPosition
            val db = dbList[pos]
            click?.invoke(pos, db)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setExtraMarginForFirstAndLastItem(position == 0, position == dbList.size - 1)
        val dbFile = dbList[position]
        holder.dbNameText.text = dbFile.name
        holder.lock.visible = dbFile.lock
    }

    override fun getItemCount() = dbList.size

}
