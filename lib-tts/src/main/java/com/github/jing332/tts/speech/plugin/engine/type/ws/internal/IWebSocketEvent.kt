package com.github.jing332.tts.speech.plugin.engine.type.ws.internal

import okhttp3.Response
import okio.ByteString

interface IWebSocketEvent {
    fun onOpen(response: Response) {}
    fun onMessage(text: String)
    fun onMessage(bytes: ByteString)
    fun onClosed(code: Int, reason: String)
    fun onClosing(code: Int, reason: String)
    fun onFailure(t: Throwable)
}