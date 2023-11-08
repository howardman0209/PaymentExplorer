package com.mobile.gateway.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mobile.gateway.model.PendingLog

@Database(entities = [PendingLog::class], version = 1)
abstract class PendingLogDatabase : RoomDatabase() {
    abstract fun pendingLogDao(): PendingLogDao
}