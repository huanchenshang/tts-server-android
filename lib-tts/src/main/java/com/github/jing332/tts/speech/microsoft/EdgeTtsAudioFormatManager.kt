package com.github.jing332.tts.speech.microsoft

import com.github.jing332.database.entities.systts.BaseAudioFormat

object EdgeTtsAudioFormatManager {
    fun getAudioFormat(format: String): BaseAudioFormat? {
        return when (format) {
            "audio-24khz-96kbitrate-mono-mp3" -> BaseAudioFormat(sampleRate = 24000)
            "audio-24khz-48kbitrate-mono-mp3" -> BaseAudioFormat(sampleRate = 24000)
            "webm-24khz-16bit-mono-opus" -> BaseAudioFormat(sampleRate = 24000 * 2)

            else -> null
        }
    }
}