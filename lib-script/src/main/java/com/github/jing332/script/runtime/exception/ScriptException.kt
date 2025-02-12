package com.github.jing332.script.runtime.exception

class ScriptException(
    val sourceName: String = "",
    val lineNumber: Int = 0,
    val columnNumber: Int = 0,

    override val message: String? = "",
    override val cause: Throwable? = null
) : RuntimeException() {}