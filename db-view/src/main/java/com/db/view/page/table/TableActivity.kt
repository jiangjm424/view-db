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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.db.view.R
import com.db.view.base.BaseBindingActivity
import com.db.view.databinding.LayoutActivityTableBinding
import com.db.view.model.Resource
import com.db.view.model.Table
import com.db.view.page.data.DataActivity

/**
 * Table layer of Activity, which shows all tables in a specific database file.
 *
 * @author guolin
 * @since 2020/9/4
 */
class TableActivity : BaseBindingActivity<LayoutActivityTableBinding>() {

    companion object {
        private const val TAG = "TableActivity"
        private const val DB_NAME = "db_name"
        private const val DB_PATH = "db_path"

        fun actionOpenDatabase(context: Context, dbName: String) {
            val intent = Intent(context, TableActivity::class.java)
            intent.putExtra(DB_NAME, dbName)
            context.startActivity(intent)
        }
    }

    private val viewModel by viewModels<TableViewModel>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbName = intent.getStringExtra(DB_NAME)
        if (dbName == null) {
            Toast.makeText(this, R.string.database_db_path_is_null, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setSupportActionBar(viewBinding.toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = dbName

        val tableList = ArrayList<Table>()
        val adapter = TableAdapter(tableList) { pos, item ->
            Log.i(TAG, "click table: $item, pos:$pos")
            DataActivity.actionOpenTable(this, item.name)
        }
        val layoutManager = LinearLayoutManager(this)
//        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.layoutManager = layoutManager

        viewModel.tablesLiveData.observe(this) {
            when (it.status) {
                Resource.SUCCESS -> {
                    viewBinding.loadingGroup.visibility = View.INVISIBLE
                    viewBinding.contentGroup.visibility = View.VISIBLE
                    tableList.addAll(it.data!!)
                    adapter.notifyDataSetChanged()
                }
                Resource.LOADING -> {
                    viewBinding.loadingGroup.visibility = View.VISIBLE
                    viewBinding.contentGroup.visibility = View.INVISIBLE
                }
                Resource.ERROR -> {
                    viewBinding.loadingGroup.visibility = View.INVISIBLE
                    viewBinding.contentGroup.visibility = View.INVISIBLE
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (viewModel.tablesLiveData.value == null) {
            viewModel.getAllTablesInDB(dbName)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
