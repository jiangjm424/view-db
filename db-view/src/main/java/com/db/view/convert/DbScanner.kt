package com.db.view.convert

import com.db.runtime.core.DataBaseImpl
import com.db.view.model.DBFile
import java.util.*

class DbScanner(private val proxy: DataBaseImpl) {
    fun loadCachedDbFiles(): List<DBFile> {
        return emptyList()
    }

    fun loadAllDbFiles(): List<DBFile> {
        val dbResp = proxy.getDBListResponse()
        val dbResultList = mutableListOf<DBFile>()
        if (dbResp.isSuccessful) {
            dbResp.rows.forEach {
                val dbItem = it as Array<String>
                val dbName = dbItem.firstOrNull()
                val dbPwd = dbItem[1].toBoolean()
                if (dbName != null) {
                    if (dbName.endsWith("-wal") or dbName.endsWith("-shm") or dbName.endsWith("-journal")) return@forEach
                    dbResultList.add(DBFile(dbName, dbPwd, true, Date()))
                }
            }
        }
        return dbResultList
    }

    fun cacheDbFiles(scannedDBList: List<DBFile>?) {
    }
}
