package com.db.view.convert

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.widget.TextView
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.db.runtime.core.DBViewCore
import com.db.runtime.core.DataBaseImpl
import com.db.view.extfuns.dp
import com.db.view.model.Column
import com.db.view.model.Data
import com.db.view.model.Row
import com.db.view.model.Table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class TableScanner(private val proxy: DataBaseImpl) {
    companion object {
        private const val TAG = "TableScanner"
        private const val CMD_TABLES = "getTableList?database="
        private const val CMD_TABLE_INFO = "getAllDataFromTheTable?tableName="
        private const val CMD_TABLE_DATA = "getAllDataFromTheTable?tableName="
    }

    fun getSortedTablesInDB(dbName: String): List<Table> {
        val sql = CMD_TABLES + dbName
        val resp = proxy.getTableListResponse(sql)
        val l = mutableListOf<Table>()
        if (resp.isSuccessful) {
            resp.rows.forEach {
                val s = it as String
                l.add(Table((s)))
            }
        }
        Log.i(TAG, "found ${l.size} tables in db:$dbName")
        return l
    }

    fun closeDatabase() {
        proxy.closeDatabase()
    }

    //todo: 获取table的列名
    suspend fun getColumnsInTable(table: String): List<Column> {
        val sql = CMD_TABLE_INFO + table
        val ll = mutableListOf<Column>()
        proxy.getTableColumns(sql)?.forEach {
            ll.add(Column(it.title ?: "<NA>", it.title ?: "<NA>", it.isPrimary))
        }
        measureColumnsWidth(null, sql, ll)
        return ll
    }

    fun updateDataInTableByPrimaryKey(
        table: String,
        primaryKey: Data,
        updateColumnName: String,
        updateColumnType: String,
        updateValue: String
    ): Int {
        return 2
    }

    //触发读表中的数据
    fun getDataFromTableStream(table: String, columns: List<Column>): Flow<PagingData<Row>> {
        val sql = CMD_TABLE_DATA + table
        return Pager(
            config = PagingConfig(10),
            pagingSourceFactory = { DBPagingSource(proxy, sql, columns) }).flow
    }

    /**
     * Measure the proper width of each column. They should just wrap the text content, but they can't
     * be smaller than the min width or larger than the max width.
     */
    /**
     * The max width of a column can be.
     */
    private val maxColumnWidth = 300.dp

    /**
     * The min width of a column can be.
     */
    private val minColumnWidth = 20.dp
    private suspend fun measureColumnsWidth(
        db: SQLiteDatabase?,
        table: String,
        columns: List<Column>
    ) = withContext(
        Dispatchers.Default
    ) {
        val loadTableDataHelper = LoadTableDataHelper(proxy)
        val paint = TextView(DBViewCore.context).paint
        for (column in columns) {
            var columnWidth = paint.measureText(column.name).toInt()
            columnWidth = min(columnWidth, maxColumnWidth)
            columnWidth = max(columnWidth, minColumnWidth)
            column.width = columnWidth
        }
        val rowList = loadTableDataHelper.loadTableData(table, 30, columns) // load 30 data
        // we iterate the first page data and evaluate the proper width of each column.
        for (row in rowList) {
            row.dataList.forEachIndexed { index, data ->
                val column = columns[index]
                var columnWidth = paint.measureText(data.value).toInt()
                columnWidth = min(columnWidth, maxColumnWidth)
                columnWidth = max(columnWidth, minColumnWidth)
                if (columnWidth > column.width) {
                    column.width = columnWidth
                }
            }
        }
    }
}
