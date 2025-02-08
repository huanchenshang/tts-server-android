package com.github.jing332.tts.manager

import com.github.jing332.database.entities.systts.BgmConfiguration

interface ITtsRepository {
    fun onInit()
    fun onDestroy()

    fun getAllTts(): Map<Long, TtsConfiguration>
    fun getAllBgm(): Map<Long, BgmConfiguration>
}