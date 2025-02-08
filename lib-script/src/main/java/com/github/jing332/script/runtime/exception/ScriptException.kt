package com.github.jing332.script.runtime.exception

class ScriptException(override val message: String? = "", override val cause: Throwable? = null) :
    RuntimeException() {
}