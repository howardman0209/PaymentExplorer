package com.payment.explorer.server

interface ServerDelegate {
    fun startServer(wait:Boolean)
    fun stopServer()
    fun getStatus(): ServerStatus
}