package com.github.jing332.tts.manager

import com.github.jing332.tts.ResultProcessorError
import com.github.michaelbull.result.Result
import java.io.InputStream

interface IResultProcessor {
    suspend fun processStream(
        ins: InputStream,
        tts: TtsConfiguration,
        targetSampleRate: Int,
        callback: IPcmAudioCallback
    ): Result<Unit, ResultProcessorError>
}