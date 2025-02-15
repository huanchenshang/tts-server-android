package com.github.jing332.script.exception

class NetworkException(override val message: String?, override val cause: Throwable?) :
    ScriptException(message = message, cause = cause) {
}