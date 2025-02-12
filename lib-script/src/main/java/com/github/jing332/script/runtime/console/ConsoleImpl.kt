package com.github.jing332.script.runtime.console

import com.github.jing332.common.LogEntry

abstract class ConsoleImpl : AbstractConsole(), LogListenerManager {
    private val listeners = mutableListOf<LogListener>()

    @Synchronized
    override fun addLogListener(listener: LogListener) {
        listeners.add(listener)
    }

    @Synchronized
    override fun removeLogListener(listener: LogListener) {
        listeners.remove(listener)
    }

    override fun println(level: Int, charSequence: CharSequence): String? {
        val log = charSequence.toString()
        listeners.forEach {
            it.onNewLog(LogEntry(level = level, message = log, wrapLine = true))
        }
        return log
    }

    override fun write(level: Int, data: CharSequence?) {
        listeners.forEach {
            it.onNewLog(LogEntry(level = level, message = data.toString(), wrapLine = false))
        }
    }

}