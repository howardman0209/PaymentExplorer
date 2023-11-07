package com.mobile.gateway.server

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.net.ServerSocket

abstract class BasicServer<T> : ServerDelegate {
    val jsonFormatter: Gson = GsonBuilder().setPrettyPrinting().create()
    var serverSocket: ServerSocket? = null
    open var server: T? = null
    override fun getStatus(): ServerStatus {
        return if (server != null) ServerStatus.RUNNING else ServerStatus.STOPPED
    }

    override fun stopServer() {
        serverSocket = null
        server = null
    }
}