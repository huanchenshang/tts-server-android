package com.github.jing332.tts.exception

class LoadException(override val message: String? = null, override val cause: Throwable? = null) :
    TtsException() {
}