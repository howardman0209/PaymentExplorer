package com.mobile.gateway.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.gateway.model.PendingLog
import com.mobile.gateway.util.roomDbTableNamePendingLog

@Dao
interface PendingLogDao {
    @Query("SELECT * FROM $roomDbTableNamePendingLog")
    fun getAllPendingLog(): List<PendingLog>?

    @Insert
    fun savePendingLog(log: PendingLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllPendingLog(logs: List<PendingLog>)

    @Query("DELETE FROM $roomDbTableNamePendingLog")
    fun removeAllPendingLog()

    @Query("DELETE FROM $roomDbTableNamePendingLog WHERE timestamp = :timestamp")
    fun removePendingLog(timestamp: Long?): Int
}