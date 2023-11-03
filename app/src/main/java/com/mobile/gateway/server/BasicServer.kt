package com.mobile.gateway.server

interface BasicServer {
    fun getInstance(): Any?
    fun startServer(wait:Boolean)
    fun stopServer()
}