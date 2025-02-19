package com.github.jing332.tts.manager

import java.nio.ByteBuffer

fun interface PcmAudioDataListener {
    fun receive(data: ByteBuffer)
}