package com.github.jing332.tts.manager

import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.BaseAudioFormat
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.ITtsSource

data class TtsConfiguration(
    val speechInfo: SpeechRuleInfo = SpeechRuleInfo(),
    val audioParams: AudioParams = AudioParams(),
    val audioFormat: BaseAudioFormat = BaseAudioFormat(),
    val source: ITtsSource,

    val standbyConfig: TtsConfiguration? = null
) {
    companion object {
        fun TtsConfigurationDTO.toVO(): TtsConfiguration {
            return TtsConfiguration(
                speechInfo = speechRule,
                audioParams = audioParams,
                audioFormat = audioFormat,
                source = source,
                standbyConfig = null,
            )
        }

        fun TtsConfiguration.toDTO(): TtsConfigurationDTO {
            return TtsConfigurationDTO(
                speechRule = speechInfo,
                audioParams = audioParams,
                audioFormat = audioFormat,
                source = source,
            )
        }
    }
}