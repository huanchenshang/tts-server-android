package com.github.jing332.script.exception

open class ScriptException(
    var sourceName: String = "",
    var lineNumber: Int = 0,
    var columnNumber: Int = 0,

    override val message: String? = "",
    override val cause: Throwable? = null
) : RuntimeException() {}