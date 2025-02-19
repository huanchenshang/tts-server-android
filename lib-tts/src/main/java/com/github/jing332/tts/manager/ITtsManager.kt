package com.github.jing332.tts.manager

import com.github.jing332.tts.error.SynthesisError
import com.github.michaelbull.result.Result


interface ITtsManager {
    val isSynthesizing: Boolean
    suspend fun init()
    suspend fun destroy()


    suspend fun synthesize(
        params: SystemParams,
        forceConfigId: Long? = null,
        callback: SynthesisCallback,
    ): Result<Unit, SynthesisError>
}

interface SynthesisCallback {
    fun onSynthesizeStart(sampleRate: Int)
    fun onSynthesizeAvailable(audio: ByteArray)
}
