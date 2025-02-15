package com.github.jing332.script.runtime.console

import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel

class Console : LogListenerManager, Writeable {
    private val listeners = mutableListOf<LogListener>()

    @Synchronized
    override fun addLogListener(listener: LogListener) {
        listeners.add(listener)
    }

    @Synchronized
    override fun removeLogListener(listener: LogListener) {
        listeners.remove(listener)
    }

    override fun write(@LogLevel level: Int, str: String) {
        listeners.forEach {
            it.onNewLog(LogEntry(level, str))
        }
    }

    fun println(str: String?) = write(LogLevel.INFO, str ?: "null")
    fun debug(str: String?) = write(LogLevel.DEBUG, str ?: "null")
    fun info(str: String?) = write(LogLevel.INFO, str ?: "null")
    fun warn(str: String?) = write(LogLevel.WARN, str ?: "null")
    fun error(str: String?) = write(LogLevel.ERROR, str ?: "null")
}