package com.mobile.gateway.room

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.mobile.gateway.MainApplication
import com.mobile.gateway.model.PendingLog
import com.mobile.gateway.util.roomDbNameMobileGatewayDb

class PendingLogRepository private constructor(private val database: PendingLogDatabase) {
    companion object {

        private var pendingLogRepository: PendingLogRepository? = null
        fun getInstance(context: Context): PendingLogRepository {
            if (pendingLogRepository == null) {
                pendingLogRepository = PendingLogRepository(MainApplication.getDatabase(context, roomDbNameMobileGatewayDb))
            }

            return pendingLogRepository!!
        }
    }

    val pendingLogList = MutableLiveData<List<PendingLog>>()

    /**
     * This function fetch data from DB and take times
     */
    private fun fetchData() {
        Log.d("PendingLogRepository", "fetch data")
        val list = database.pendingLogDao().getAllPendingLog()
        list?.let {
            pendingLogList.postValue(it)
        }
    }

    fun getAllPendingLog() {
        fetchData()
    }

    fun removePendingLog(uuid: Long?) {
        database.pendingLogDao().removePendingLog(uuid)
        fetchData()
    }

    fun removeAllPendingLog() {
        database.pendingLogDao().removeAllPendingLog()
        fetchData()
    }

    fun savePendingLog(log: PendingLog) {
        database.pendingLogDao().savePendingLog(log)
        fetchData()
    }

    fun saveAllPendingLog(logs: List<PendingLog>) {
        database.pendingLogDao().saveAllPendingLog(logs)
        fetchData()
    }
}