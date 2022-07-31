package com.db.view.page.db

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.db.runtime.core.DBViewCore
import com.db.runtime.core.ServiceLocator
import com.db.view.model.DBFile
import kotlinx.coroutines.launch

class DBViewModel(application: Application) : AndroidViewModel(application) {
    private val _urlServer = MutableLiveData<String>()
    val urlServer: LiveData<String> = _urlServer

    init {
        _urlServer.value = DBViewCore.getAddressLog()
    }


    private val repository = ServiceLocator.providerDbScanner()

    /**
     * The LiveData variable to observe db file list.
     */
    val dbListLiveData: LiveData<List<DBFile>>
        get() = _dbListLiveData

    private val _dbListLiveData = MutableLiveData<List<DBFile>>()

    /**
     * The LiveData variable to observe loading status.
     */
    val progressLiveData: LiveData<Boolean>
        get() = _progressLiveData

    private val _progressLiveData = MutableLiveData<Boolean>()

    /**
     * Load the db files from cache immediately and show them on UI.
     * Then scan all db files of current app.
     */
    fun loadAndRefreshDBFiles() = viewModelScope.launch {
        _progressLiveData.value = true // start loading
        // Load db files from cache and show the on UI immediately.
        val cachedDBList = repository.loadCachedDbFiles()
        _dbListLiveData.value = cachedDBList
        _progressLiveData.value = false // finish loading
        refreshDBFiles()
    }

    /**
     * Scan all db files of current app, then refresh the ui of current app.
     */
    fun refreshDBFiles() = viewModelScope.launch {
        _progressLiveData.value = true // start loading
        // Scan all db files of current app and update the UI with DiffUtil.
        val scannedDBList = repository.loadAllDbFiles()
        _dbListLiveData.value = scannedDBList

        // Update the cache with lasted data.
        repository.cacheDbFiles(scannedDBList)
        _progressLiveData.value = false // finish loading
    }
}
