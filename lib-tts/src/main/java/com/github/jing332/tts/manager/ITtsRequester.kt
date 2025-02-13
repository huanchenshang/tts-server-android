package com.github.jing332.tts.manager

import com.github.jing332.tts.RequesterError
import java.io.InputStream

interface ITtsRequester {
    suspend fun request(
        params: SystemParams,
        tts: TtsConfiguration
    ): com.github.michaelbull.result.Result<Result, RequesterError>

    fun destroy()

    data class Result @JvmOverloads constructor(
        val callback: ISyncPlayCallback? = null,
        val stream: InputStream? = null
    ) {
        inline fun <R> onStream(block: (InputStream) -> R): Result {
            if (stream != null) block.invoke(stream)

            return this
        }

        inline fun <R> onCallback(block: (ISyncPlayCallback) -> R): Result {
            if (callback != null) block.invoke(callback)

            return this
        }
    }

    fun interface ISyncPlayCallback {
        suspend fun play()
    }
}

