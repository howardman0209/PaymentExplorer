package com.mobile.gateway.server

interface ServerDelegate {
    fun startServer(wait:Boolean)
    fun stopServer()
    fun getStatus(): ServerStatus
}