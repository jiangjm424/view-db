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

package com.db.view.page.data

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.db.view.R
import com.db.view.base.BaseBindingActivity
import com.db.view.databinding.LayoutActivityDataBinding
import com.db.view.extfuns.dp
import com.db.view.model.Column
import com.db.view.model.Resource
import com.db.view.model.Row
import com.db.view.model.UpdateBean
import com.db.view.widget.TableCellView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

/**
 * Data layer of Activity, which shows data from a table by page.
 *
 * @author guolin
 * @since 2020/9/13
 */
class DataActivity : BaseBindingActivity<LayoutActivityDataBinding>(),
    DataAdapter.OnCellDoubleClickListener {

    companion object {
        private const val TAG = "DataActivity"
        private const val TABLE_NAME = "table_name"
        fun actionOpenTable(context: Context, tableName: String) {
            val intent = Intent(context, DataActivity::class.java)
            intent.putExtra(TABLE_NAME, tableName)
            context.startActivity(intent)
        }
    }

    private val viewModel by viewModels<DataViewModel>()

    /**
     * The adapter for the main data of table.
     */
    private lateinit var adapter: DataAdapter

    /**
     * The adapter for the footer view.
     */
    private lateinit var footerAdapter: DataFooterAdapter

    /**
     * The table which need to show its data.
     */
    private lateinit var table: String

    /**
     * Indicates the load is started or not.
     */
    private var loadStarted = false

    /**
     * Dialog with EditText to modify value.
     */
    private var editDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tableName = intent.getStringExtra(TABLE_NAME)
        if (tableName == null) {
            Toast.makeText(this, R.string.database_table_name_is_null, Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }
        table = tableName

        setSupportActionBar(viewBinding.toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = table

        val layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.layoutManager = layoutManager

        viewModel.errorLiveData.observe(this) {
            Toast.makeText(
                this,
                it?.message
                    ?: baseContext.getString(R.string.database_uncaught_exception_happened),
                Toast.LENGTH_SHORT
            ).show()
            viewBinding.progressBar.visibility = View.INVISIBLE
        }
        viewModel.columnsLiveData.observe(this) {
            initAdapter(table, it)
        }
        viewModel.updateDataLiveData.observe(this) {
            when (it.status) {
                Resource.ERROR -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
                Resource.SUCCESS -> {
                    val updateBean = it.data!!
                    updateBean.row.dataList[updateBean.columnIndex].value = updateBean.updateValue
                    adapter.notifyItemChanged(updateBean.position)
                    Toast.makeText(
                        this,
                        R.string.database_update_succeeded,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        if (viewModel.columnsLiveData.value == null) {
            viewModel.getColumnsInTable(table)
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

    override fun onDestroy() {
        super.onDestroy()
        editDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    /**
     * Show the dialog with existing value from db, and provide a editable way to change it.
     */
    private fun showModifyValueDialog(position: Int, row: Row, columnIndex: Int) {
        if (!::adapter.isInitialized) return
        require(columnIndex >= 0) {
            "You're not editing a valid column with index -1."
        }
        val updateColumnType = row.dataList[columnIndex].columnType
        //todo: jiang
//        if (updateColumnType == BLOB_FIELD_TYPE) {
//            Toast.makeText(this, R.string.database_blob_column_can_not_be_modified, Toast.LENGTH_SHORT).show()
//            return
//        }
        editDialog = AlertDialog.Builder(this).apply {
            setView(R.layout.layout_dialog_edit_text)
//            setPositiveButton(R.string.database_apply) { _, _ ->
//                applyValueModification(position, row, columnIndex)
//            }
            setNegativeButton(R.string.database_apply, null)
        }.create()
        editDialog?.let {
            it.show()
//            it.getButton(AlertDialog.BUTTON_POSITIVE)
//                .setTextColor(ContextCompat.getColor(this, R.color.database_positive_button))
            it.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.database_positive_button))
            val textView = it.findViewById<TextView>(R.id.dialog_edit_text)
            require(textView != null) {
                "dialogEditText shouldn't be null at this time."
            }
            textView.text = row.dataList[columnIndex].value
            textView.requestFocusFromTouch()
        }
    }

    /**
     * Init the adapter for displaying data in RecyclerView.
     */
    private fun initAdapter(table: String, columns: List<Column>) {
        // It may cost some time for calculating row width, so we put it into thread.
        thread {
            var rowWidth = 0
            for (column in columns) {
                rowWidth += column.width
            }
            rowWidth += columns.size * 20.dp // we always have 20dp extra space for each column. 5dp for start. 15dp for end.
            viewBinding.recyclerView.post {
                // Make sure we are back to the main thread and we can get the width of HorizontalScroller now.
                rowWidth = rowWidth.coerceAtLeast(viewBinding.horizontalScroller.width)
                buildTableTitle(columns, rowWidth)
                adapter = DataAdapter(this, columns, rowWidth)
                footerAdapter = DataFooterAdapter(rowWidth, viewBinding.horizontalScroller.width) {
                    adapter.itemCount
                }
//                adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                // Concat DataAdapter and DataFooterAdapter which can show how many records loaded at bottom.
                viewBinding.recyclerView.adapter = ConcatAdapter(adapter, footerAdapter)
                adapter.addLoadStateListener { loadState ->
                    when (loadState.refresh) {
                        is LoadState.NotLoading -> {
                            viewBinding.horizontalScroller.visibility = View.VISIBLE
                            viewBinding.progressBar.visibility = View.INVISIBLE
                            if (loadStarted) {
                                // This case may be invoked before loading start.
                                // We only display footer view after first loading by paging3.
                                // Otherwise RecyclerView will scroll to the bottom when load finished which we don't want to see.
                                footerAdapter.displayFooter()
                            }
                        }
                        is LoadState.Loading -> {
                            viewBinding.horizontalScroller.visibility = View.INVISIBLE
                            viewBinding.progressBar.visibility = View.VISIBLE
                        }
                        is LoadState.Error -> {
                            viewBinding.horizontalScroller.visibility = View.VISIBLE
                            viewBinding.progressBar.visibility = View.INVISIBLE
                            Toast.makeText(
                                this, R.string.database_something_is_wrong_when_loading_data,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                // Listener the scroll amount by the HorizontalScroller and notify it to DataFooterAdapter
                // to make the text on footer view showed in center.
                viewBinding.horizontalScroller.setScrollObserver {
                    footerAdapter.notifyScrollXChanged(it)
                }
                loadDataFromTable(table, columns)
            }
        }
    }

    /**
     * Begin to load data from table with the specific columns.
     */
    private fun loadDataFromTable(table: String, columns: List<Column>) {
        lifecycleScope.launch {
            viewModel.loadDataFromTable(table, columns).collect {
                loadStarted = true
                adapter.submitData(it)
                // This line will never reach. Don't do anything below.
            }
            // This line will never reach. Don't do anything below.
        }
    }

    /**
     * Build a TableRowLayout as a row to show the columns of a table as title.
     */
    private fun buildTableTitle(columns: List<Column>, rowWidth: Int) {
        val param = viewBinding.rowTitleLayout.layoutParams
        param.width = rowWidth
        columns.forEachIndexed { index, column ->
            val tableCellView = buildTableCellView(column)
            tableCellView.columnIndex = index // Indicate the column index of the row.
            // We let each column has 20dp extra space, to make it look better.
            val layoutParam = LinearLayout.LayoutParams(
                column.width + 20.dp,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            viewBinding.rowTitleLayout.addView(tableCellView, layoutParam)
        }
        viewBinding.rowTitleLayout.setBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.database_table_even_row_bg
            )
        )
    }

    /**
     * Build a TableCellView widget as a table cell to show data in title.
     */
    private fun buildTableCellView(column: Column): TableCellView {
        val tableCellView = TableCellView(this)
        tableCellView.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
        tableCellView.setTextColor(ContextCompat.getColor(this, R.color.database_table_text))
        tableCellView.typeface = Typeface.DEFAULT_BOLD
        tableCellView.text = column.name
        return tableCellView
    }

    /**
     * Apply the value modification into database, then update the UI with new value.
     */
    private fun applyValueModification(position: Int, row: Row, columnIndex: Int) =
        editDialog?.let {
            val dialogEditText = it.findViewById<EditText>(R.id.dialog_edit_text)
            require(dialogEditText != null) {
                "dialogEditText shouldn't be null at this time."
            }
            val newValue = dialogEditText.text.toString()
            viewModel.updateDataInTable(UpdateBean(table, row, position, columnIndex, newValue))
        }

    override fun onCellDoubleClickListener(position: Int, row: Row, columnIndex: Int) {
        showModifyValueDialog(position, row, columnIndex)
    }


}
