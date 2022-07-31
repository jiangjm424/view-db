package com.grank.db.demo.db.user

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grank.db.demo.db.ioThread

/**
 * Created by anandgaurav on 12/02/18.
 */
@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class UserDb : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        private var instance: UserDb? = null

        @Synchronized
        fun get(context: Context): UserDb {
            if (instance == null) {
                Log.i("jiang","init user db")
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDb::class.java, "User.db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        fillInDb(context.applicationContext)
                    }
                }).build()
            }
            return instance!!
        }

        /**
         * fill database with list of cheeses
         */
        private fun fillInDb(context: Context) {
            // inserts in Room are executed on the current thread, so we insert in the background
            ioThread {
                repeat(2) {
                    get(context).userDao().insertL(User(null, "user:$it"))
                }
            }
        }
    }
}
