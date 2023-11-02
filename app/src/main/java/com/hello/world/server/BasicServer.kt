package com.hello.world.server

interface BasicServer {
    fun getInstance(): Any?
    fun startServer(wait:Boolean)
    fun stopServer()
}