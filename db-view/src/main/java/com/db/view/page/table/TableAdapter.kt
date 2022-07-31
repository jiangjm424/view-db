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

package com.db.view.page.table

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.db.view.databinding.LayoutTableItemBinding
import com.db.view.extfuns.setExtraMarginForFirstAndLastItem
import com.db.view.model.DBFile
import com.db.view.model.Table

/**
 * Adapter for the RecyclerView to show all tables in db file.
 *
 * @author guolin
 * @since 2020/9/10
 */
class TableAdapter(
    private val tableList: List<Table>,
    private val click: ((Int, Table) -> Unit)? = null
) : RecyclerView.Adapter<TableAdapter.ViewHolder>() {

    class ViewHolder(tableItemBinding: LayoutTableItemBinding) :
        RecyclerView.ViewHolder(tableItemBinding.root) {
        val tableNameText: TextView = tableItemBinding.tableNameText
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tableItemBinding =
            LayoutTableItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(tableItemBinding)
        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            val item = tableList[pos]
            click?.invoke(pos, item)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setExtraMarginForFirstAndLastItem(position == 0, position == tableList.size - 1)
        val table = tableList[position]
        holder.tableNameText.text = table.name
    }

    override fun getItemCount() = tableList.size

}
