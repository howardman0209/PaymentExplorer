package com.payment.explorer.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.payment.explorer.util.roomDbTableNamePendingLog

@Entity(tableName = roomDbTableNamePendingLog)
data class PendingLog(
    @PrimaryKey
    val timestamp: Long,
    val message: String
)
