package com.github.jing332.script.runtime.console

import android.util.Log


abstract class AbstractConsole : Console {

    fun printf(level: Int, data: Any?, vararg formatArgs: Any?) {
        println(level, format(data, *formatArgs))
    }

    fun format(data: Any?, vararg formatArgs: Any?): CharSequence {
        data ?: return "\n"
        return when (formatArgs.isEmpty()) {
            true -> data.toString()
            else -> String.format(data.toString(), *formatArgs)
        }
    }

    protected abstract fun write(level: Int, data: CharSequence?)

    override fun print(level: Int, data: Any?, vararg formatArgs: Any?) {
        write(level, format(data, *formatArgs))
    }

    override fun verbose(data: Any?, vararg formatArgs: Any?) {
        printf(Log.VERBOSE, data, *formatArgs)
    }

    override fun log(data: Any?, vararg formatArgs: Any?) {
        printf(Log.DEBUG, data, *formatArgs)
    }

    override fun debug(data: Any?, vararg formatArgs: Any?) {
        printf(Log.DEBUG, data, *formatArgs)
    }

    override fun info(data: Any?, vararg formatArgs: Any?) {
        printf(Log.INFO, data, *formatArgs)
    }

    override fun warn(data: Any?, vararg formatArgs: Any?) {
        printf(Log.WARN, data, *formatArgs)
    }

    override fun error(data: Any?, vararg formatArgs: Any?) {
        printf(Log.ERROR, data, *formatArgs)
    }

}