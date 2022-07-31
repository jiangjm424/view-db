package com.grank.db.demo.db.user

import androidx.room.*
import com.grank.db.demo.db.user.UserDao

/**
 * Created by anandgaurav on 12/02/18.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun loadAll(): List<User>

    @Query("SELECT * FROM users WHERE id IN (:userIds)")
    suspend fun loadAllByIds(userIds: List<Int?>?): List<User?>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertL(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Delete
    suspend fun delete(user: User?)
}
