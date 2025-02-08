package com.github.jing332.tts.manager

import com.github.jing332.tts.TtsError
import com.github.michaelbull.result.Result


interface ITtsManager {
    val isSynthesizing: Boolean
    fun init(): Result<Unit, TtsError>
    fun destroy()
    suspend fun synthesize(
        params: SystemParams,
        forceConfigId: Long? = null,
        callback: SynthesisCallback
    ): com.github.michaelbull.result.Result<Unit, TtsError>
}

interface SynthesisCallback {
    fun onSynthesizeStart(sampleRate: Int)
    fun onSynthesizeError(code: Int, reason: Exception? = null)
    fun onSynthesizeFinish()
    fun onSynthesizeAvailable(audio: ByteArray)
}
