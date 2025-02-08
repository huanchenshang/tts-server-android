package com.github.jing332.server

interface Server {
    fun start(wait: Boolean)
    fun stop()
}