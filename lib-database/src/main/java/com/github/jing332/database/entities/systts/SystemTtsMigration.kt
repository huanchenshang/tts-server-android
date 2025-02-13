package com.github.jing332.database.entities.systts

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.v1.SystemTts
import com.github.jing332.database.entities.systts.v1.tts.BgmTTS
import com.github.jing332.database.entities.systts.v1.tts.LocalTTS
import com.github.jing332.database.entities.systts.v1.tts.MsTTS

object SystemTtsMigration {
    fun v1Tov2(v1: SystemTts): SystemTtsV2? {
        val config = if (v1.tts is BgmTTS) BgmConfiguration(
            musicList = (v1.tts as BgmTTS).musicList.toList(),
            volume = v1.tts.volume / 1000f
        )
        else
            TtsConfigurationDTO(
                speechRule = v1.speechRule,
                audioParams = v1.tts.audioParams,
                audioFormat = v1.tts.audioFormat,
                source = when (v1.tts) {
                    is LocalTTS -> {
                        val tts = v1.tts as LocalTTS
                        LocalTtsSource(
                            engine = tts.engine ?: "",
                            voice = tts.voiceName ?: "",
//                            speed = tts.rate,
//                            pitch = tts.pitch,
                            extraParams = tts.extraParams,
                            isDirectPlayMode = tts.isDirectPlayMode
                        )
                    }


//                        is HttpTTS -> TODO()
//                        is PluginTTS -> TODO()
                    else -> return null
                }
            )


        return SystemTtsV2(
            id = v1.id,
            displayName = v1.displayName ?: "",
            groupId = v1.groupId,
            isEnabled = v1.isEnabled,
            order = v1.order,
            config = config
        )
    }


    fun migrate() {
        dbm.systemTtsDao.allTts.forEach {
            dbm.systemTtsV2.insert(v1Tov2(it) ?: return@forEach)
        }
    }
}