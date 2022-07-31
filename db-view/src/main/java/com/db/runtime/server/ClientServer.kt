/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */
package com.db.runtime.server

/**
 * Created by amitshekhar on 15/11/16.
 */
import android.content.Context
import android.util.Log
import com.db.runtime.core.DataBaseImpl
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketException

class ClientServer(context: Context, port: Int, dbHelper: DataBaseImpl) : Runnable {
    private val mRequestHandler: RequestHandler = RequestHandler(context, dbHelper)
    private val mPort = port
    var isRunning = false
        private set
    private var mServerSocket: ServerSocket? = null
    fun start() {
        isRunning = true
        Thread(this).start()
    }

    fun stop() {
        try {
            isRunning = false
            if (null != mServerSocket) {
                mServerSocket!!.close()
                mServerSocket = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing the server socket.", e)
        }
    }

    override fun run() {
        try {
            mServerSocket = ServerSocket(mPort)
            while (isRunning) {
                val socket = mServerSocket!!.accept()
                mRequestHandler.handle(socket)
                socket.close()
            }
        } catch (e: SocketException) {
            // The server was stopped; ignore.
        } catch (e: IOException) {
            Log.e(TAG, "Web server error.", e)
        } catch (ignore: Exception) {
            Log.e(TAG, "Exception.", ignore)
        }
    }

    companion object {
        private const val TAG = "ClientServer"
    }

}
