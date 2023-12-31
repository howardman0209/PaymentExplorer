package com.mobile.gateway.util

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mobile.gateway.extension.toDateString
import com.mobile.gateway.handler.Event
import com.mobile.gateway.model.PendingLog
import com.mobile.gateway.room.PendingLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DebugPanelManager {
    private lateinit var pendingLogRepository: PendingLogRepository
    var isForeground = true
    private val _messageToLog = MutableLiveData<Event<String>>()
    val messageToLog: LiveData<Event<String>>
        get() = _messageToLog

    private val _display = MutableLiveData<Event<Boolean>>()
    val display: LiveData<Event<Boolean>>
        get() = _display

    fun initDebugPanel(context: Context) {
        pendingLogRepository = PendingLogRepository.getInstance(context)
        log("Debug log init successfully")
    }

    fun log(message: String?) {
        if (isForeground) {
            CoroutineScope(Dispatchers.Main).launch {
                val timestamp = System.currentTimeMillis().toDateString(DATE_TIME_DISPLAY_PATTERN_SO_SHORT)
                _messageToLog.value = Event("$timestamp ${message ?: "null"}")
            }
        } else {
            if (::pendingLogRepository.isInitialized) {
                pendingLogRepository.savePendingLog(
                    PendingLog(
                        timestamp = System.currentTimeMillis(),
                        message = message ?: "null"
                    )
                )
            }
        }
    }

    fun logPendingMessage() {
        pendingLogRepository.pendingLogList.value?.forEach { pendingMsg ->
            CoroutineScope(Dispatchers.Main).launch {
                val timestamp = pendingMsg.timestamp.toDateString(DATE_TIME_DISPLAY_PATTERN_SO_SHORT)
                _messageToLog.value = Event("$timestamp ${pendingMsg.message}")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            pendingLogRepository.removeAllPendingLog()
        }
    }

    fun show(visibility: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            _display.value = Event(visibility)
        }
    }

    fun <T> safeExecute(onFail: ((ex: Exception) -> Unit)? = null, task: () -> T): T? {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            onFail?.invoke(ex)
            null
        }
    }
}