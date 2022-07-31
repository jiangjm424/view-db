package com.grank.db.demo.db.user

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by anandgaurav on 12/02/18.
 */
@Entity(tableName = "users")
data class User (
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null
)
