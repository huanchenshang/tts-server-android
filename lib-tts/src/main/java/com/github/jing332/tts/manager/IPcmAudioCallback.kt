package com.github.jing332.tts.manager

fun interface IPcmAudioCallback {
    suspend fun onPcmData(data: ByteArray)
}