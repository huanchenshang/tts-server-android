package com.github.jing332.database.entities.systts

import com.github.jing332.database.entities.systts.source.ITtsSource
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("tts")
data class TtsConfigurationDTO(
    val speechRule: SpeechRuleInfo = SpeechRuleInfo(),
    val audioParams: AudioParams = AudioParams(),
    val audioFormat: BaseAudioFormat = BaseAudioFormat(),
    val source: ITtsSource,
) : IConfiguration() {
}