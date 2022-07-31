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

import android.app.Application
import androidx.lifecycle.*
import com.db.runtime.core.ServiceLocator
import com.db.view.R
import com.db.view.convert.TableScanner
import com.db.view.model.Resource
import com.db.view.model.Table
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * TableViewModel holds view data of TableActivity and provide api to specific table operations.
 *
 * @author guolin
 * @since 2020/9/4
 */
class TableViewModel(application: Application) : AndroidViewModel(
    application
) {

    private val repository: TableScanner = ServiceLocator.providerTableScanner()
    /**
     * The LiveData variable to observe db file list.
     */
    val tablesLiveData: LiveData<Resource<List<Table>>>
        get() = _tablesLiveData

    private val _tablesLiveData = MutableLiveData<Resource<List<Table>>>()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _tablesLiveData.value = Resource.error(throwable.message
                ?: application.getString(R.string.database_uncaught_exception_happened))
    }

    /**
     * Get all tables in a specific db file represented by the [dbPath] parameter.
     */
    fun getAllTablesInDB(dbPath: String) = viewModelScope.launch(handler) {
        _tablesLiveData.value = Resource.loading()
        _tablesLiveData.value = Resource.success(repository.getSortedTablesInDB(dbPath))
    }

    /**
     * When the lifecycle of TableViewModel finished, we close the opened database.
     */
    override fun onCleared() {
        closeDatabase()
    }

    /**
     * Close the opened database.
     */
    private fun closeDatabase() = viewModelScope.launch(handler) {
        repository.closeDatabase()
    }

}
