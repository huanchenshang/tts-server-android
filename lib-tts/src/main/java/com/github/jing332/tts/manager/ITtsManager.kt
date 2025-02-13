package com.github.jing332.tts.manager

import com.github.jing332.tts.ForceConfigIdNotFound
import com.github.jing332.tts.TtsError
import com.github.michaelbull.result.Result


interface ITtsManager {
    val isSynthesizing: Boolean
    suspend fun init(): Result<Unit, TtsError>
    suspend fun destroy()

    /**
     * @return
     * [ForceConfigIdNotFound] if forceConfigId is not found from database
     *
     */
    suspend fun synthesize(
        params: SystemParams,
        forceConfigId: Long? = null,
        callback: SynthesisCallback
    ): Result<Unit, TtsError>
}

interface SynthesisCallback {
    fun onSynthesizeStart(sampleRate: Int)
    fun onSynthesizeAvailable(audio: ByteArray)
}
