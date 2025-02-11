package com.github.jing332.script.runtime.console

import com.github.jing332.script.annotation.ScriptInterface

interface Console {
    @ScriptInterface
    fun i(data: Any?) = info(data)

    @ScriptInterface
    fun d(data: Any?) = debug(data)

    @ScriptInterface
    fun w(data: Any?) = warn(data)

    @ScriptInterface
    fun e(data: Any?) = error(data)

    @ScriptInterface
    fun verbose(data: Any?, vararg formatArgs: Any?)

    @ScriptInterface
    fun verbose(data: Any?) = verbose(data, *emptyArray())

    @ScriptInterface
    fun log(data: Any?, vararg formatArgs: Any?)

    @ScriptInterface
    fun log(data: Any?) = log(data, *emptyArray())

    @ScriptInterface
    fun print(level: Int, data: Any?, vararg formatArgs: Any?)

    @ScriptInterface
    fun print(level: Int, data: Any?) = print(level, data, *emptyArray())

    @ScriptInterface
    fun debug(data: Any?, vararg formatArgs: Any?)

    @ScriptInterface
    fun debug(data: Any?) = debug(data, *emptyArray())

    @ScriptInterface
    fun info(data: Any?, vararg formatArgs: Any?)

    @ScriptInterface
    fun info(data: Any?) = info(data, *emptyArray())

    @ScriptInterface
    fun warn(data: Any?, vararg formatArgs: Any?)

    @ScriptInterface
    fun warn(data: Any?) = warn(data, *emptyArray())

    @ScriptInterface
    fun error(data: Any?, vararg formatArgs: Any?)

    @ScriptInterface
    fun error(data: Any?) = error(data, *emptyArray())

    @ScriptInterface
    fun println(level: Int, charSequence: CharSequence): String?
}