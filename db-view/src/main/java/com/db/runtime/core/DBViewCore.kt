package com.db.runtime.core

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.db.runtime.server.ClientServer
import com.db.runtime.utils.Constants
import com.db.runtime.utils.NetworkUtils

@SuppressLint("StaticFieldLeak")
object DBViewCore {
    private const val TAG = "DBViewCore"
    private const val DEFAULT_PORT = 8080
    private var clientServer: ClientServer? = null
    private var addressLog = "not available"
    internal lateinit var context: Context
    fun initialize(c: Context) {
        Log.i(Constants.TAG_DEBUG, "init db view core with: $c")
        this.context = c
        clientServer = ClientServer(c, DEFAULT_PORT, ServiceLocator.providerServerDBHelper())
        clientServer?.start()
        addressLog = NetworkUtils.getAddressLog(c, DEFAULT_PORT)
        Log.d(TAG, addressLog)
    }

    fun getAddressLog(): String {
        Log.d(TAG, addressLog)
        return addressLog
    }

    fun shutDown() {
        if (clientServer != null) {
            clientServer?.stop()
            clientServer = null
        }
    }

//    fun setCustomDatabaseFiles(customDatabaseFiles: HashMap<String, Pair<File, String>>?) {
//        if (clientServer != null) {
//            clientServer!!.setCustomDatabaseFiles(customDatabaseFiles)
//        }
//    }
//
//    fun setInMemoryRoomDatabases(databases: HashMap<String, SupportSQLiteDatabase>?) {
//        if (clientServer != null) {
//            clientServer!!.setInMemoryRoomDatabases(databases)
//        }
//    }

    val isServerRunning: Boolean
        get() = clientServer?.isRunning ?: false
}
