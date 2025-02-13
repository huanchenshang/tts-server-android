package com.github.jing332.tts.manager

import com.github.jing332.database.entities.systts.BgmConfiguration

interface ITtsRepository {
    fun init()
    fun destroy()

    fun getAllTts(): Map<Long, TtsConfiguration>
    fun getAllBgm(): List<BgmConfiguration>
}