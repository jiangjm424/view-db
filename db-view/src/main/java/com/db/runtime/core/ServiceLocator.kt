package com.db.runtime.core

import com.db.runtime.sqliteimpl.DBFactoryImpl
import com.db.view.convert.DbScanner
import com.db.view.convert.TableScanner

object ServiceLocator {
    private val dbFactory = DBFactoryImpl()

    private val serverDbImpl = DataBaseImpl(dbFactory)
    fun providerServerDBHelper() = serverDbImpl

    private val localDbImpl = DataBaseImpl(dbFactory)
    private val dbScanner = DbScanner(localDbImpl)
    private val tableScanner = TableScanner(localDbImpl)
    fun providerDbScanner() = dbScanner
    fun providerTableScanner() = tableScanner
}
