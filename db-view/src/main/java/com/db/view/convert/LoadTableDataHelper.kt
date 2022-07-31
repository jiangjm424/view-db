package com.db.view.convert

import com.db.runtime.core.DataBaseImpl
import com.db.runtime.model.TableDataResponse
import com.db.runtime.utils.DataType
import com.db.view.model.Column
import com.db.view.model.Data
import com.db.view.model.Row

class LoadTableDataHelper(private val proxy: DataBaseImpl) {
    private val page_size = 30
    fun loadTableData(table: String, size: Int, columns: List<Column>): List<Row> {
        val ll = mutableListOf<Row>()
        var count = 0
        val aa = proxy.getAllDataFromTheTableResponse(table, size)
        aa.rows?.forEach {
            val row = it as? List<TableDataResponse.ColumnData>
            row?.map { c ->

                when (c.dataType) {
                    DataType.TEXT -> {
                        val vv = c.value as? String ?: "N/A"
                        Data(vv, "", c.dataType ?: "", false)
                    }
                    else -> {
                        val vv = c.value?.toString() ?: "-N/A"
                        Data(vv, "", c.dataType ?: "", false)
                    }
                }

            }?.also { r ->
                count++
                ll.add(Row(count, r))
            }
        }

        return ll
    }
}
