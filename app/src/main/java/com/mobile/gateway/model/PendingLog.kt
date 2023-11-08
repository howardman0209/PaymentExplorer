package com.mobile.gateway.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mobile.gateway.util.roomDbTableNamePendingLog

@Entity(tableName = roomDbTableNamePendingLog)
data class PendingLog(
    @PrimaryKey
    val timestamp: Long,
    val message: String
)
