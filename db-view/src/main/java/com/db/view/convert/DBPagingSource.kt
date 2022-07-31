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

package com.db.view.convert

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.db.runtime.core.DataBaseImpl
import com.db.view.model.Column
import com.db.view.model.Row

/**
 * We need to use a DBPagingSource and inherits from PagingSource to implements the paging function with paging3 library.
 *
 * @author guolin
 * @since 2020/9/17
 */
class DBPagingSource(
    proxy: DataBaseImpl,
    private val table: String,
    private val columns: List<Column>
) : PagingSource<Int, Row>() {
    private val loadTableDataHelper = LoadTableDataHelper(proxy)
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Row> {
        return try {
            val page = params.key ?: 0 // set page 0 as default
            val rowData = loadTableDataHelper.loadTableData(table, page, columns)
            LoadResult.Page(rowData, null, null)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Row>): Int? = null


}
