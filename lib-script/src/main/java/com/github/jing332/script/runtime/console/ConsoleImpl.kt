package com.github.jing332.script.runtime.console

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
            it.onNewLog(LogEntry(log, level, wrapLine = true))
        }
        return log
    }

    override fun write(level: Int, data: CharSequence?) {
        listeners.forEach {
            it.onNewLog(LogEntry(data.toString(), level, wrapLine = false))
        }
    }

}