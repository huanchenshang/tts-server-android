package com.github.jing332.tts.manager.event

import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.TextSegment
import com.github.jing332.tts.manager.TtsConfiguration

sealed interface EventType {
    open class Error(open val cause: Throwable? = null) : EventType

    data class Request(
        val params: SystemParams,
        val config: TtsConfiguration,
        val times: Int
    ) : EventType

    data class RequestSuccess(
        val timeCost: Long,
        val params: SystemParams,
        val config: TtsConfiguration,
    ) : EventType

    data class RequestError(
        val params: SystemParams,
        val config: TtsConfiguration,
        override val cause: Throwable?
    ) : Error(cause)

    data class RequestTimeout(
        val params: SystemParams,
        val config: TtsConfiguration,
    ) : EventType

    data class TextProcessorError(
        val text: String,
        override val cause: Throwable?
    ) : Error(cause)

    data class ResultProcessorError(
        val params: SystemParams,
        val config: TtsConfiguration,
        override val cause: Throwable?
    ) : Error(cause)

    data class AudioDecodingError(
        val params: SystemParams,
        val config: TtsConfiguration,
        override val cause: Throwable?
    ) : Error(cause)

    data class AudioStreamError(
        val params: SystemParams,
        val config: TtsConfiguration,
        override val cause: Throwable?
    ) : Error(cause)

    data class DirectPlayError(
        val fragment: TextSegment,
        override val cause: Throwable?
    ) : Error(cause)

    data class DirectPlay(val fragment: TextSegment) : EventType
    data class StandbyTts(val params: SystemParams, val tts: TtsConfiguration) : EventType

    object ConfigEmptyError : Error()
    data object RequestTimesEnded : EventType
}