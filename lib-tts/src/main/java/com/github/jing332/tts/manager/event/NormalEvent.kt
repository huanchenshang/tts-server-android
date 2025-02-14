package com.github.jing332.tts.manager.event

import com.github.jing332.tts.manager.RequestPayload

sealed interface NormalEvent : Event {
    data class Request(
        val request: RequestPayload,
        val retries: Int
    ) : NormalEvent

    data class ReadAllFromStream(
        val request: RequestPayload,
        val size: Int,
        val costTime: Long
    ) : NormalEvent

    data class HandleStream(
        val request: RequestPayload
    ) : NormalEvent

    data class DirectPlay(val request: RequestPayload) : NormalEvent
    data class StandbyTts(val request: RequestPayload) : NormalEvent
    data object RequestCountEnded : NormalEvent
}