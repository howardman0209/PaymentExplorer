package com.mobile.gateway.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mobile.gateway.handler.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DebugPanelManager {
    private val _messageToLog = MutableLiveData<Event<String>>()
    val messageToLog: LiveData<Event<String>>
        get() = _messageToLog

    private val _display = MutableLiveData<Event<Boolean>>()
    val display: LiveData<Event<Boolean>>
        get() = _display

    fun log(message: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            _messageToLog.value = Event(message ?: "null")
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