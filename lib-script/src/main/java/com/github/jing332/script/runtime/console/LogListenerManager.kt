package com.github.jing332.script.runtime.console

import android.util.Log.ASSERT
import android.util.Log.DEBUG
import android.util.Log.ERROR
import android.util.Log.INFO
import android.util.Log.VERBOSE
import android.util.Log.WARN

interface LogListenerManager {
    fun addLogListener(listener: LogListener)
    fun removeLogListener(listener: LogListener)
}

fun interface LogListener {
    fun onNewLog(entry: LogEntry)

}

data class LogEntry(val message: String, val level: Int, val wrapLine: Boolean = true) {
    fun getLevelChar(): String = level.toLogLevelChar()
}

fun Int.toLogLevelChar(): String {
    return when (this) {
        VERBOSE -> "V"
        DEBUG -> "D"
        INFO -> "I"
        WARN -> "W"
        ERROR -> "E"
        ASSERT -> "A"
        else -> ""
    }
}