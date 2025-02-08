package com.github.jing332.tts.manager

import java.io.InputStream

interface IResultProcessor {
    suspend fun processStream(
        ins: InputStream,
        tts: TtsConfiguration,
        targetSampleRate: Int,
        callback: IPcmAudioCallback
    )
}