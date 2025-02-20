package com.github.jing332.database.entities.systts

import com.github.jing332.database.entities.systts.source.TextToSpeechSource
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
    val source: TextToSpeechSource,
) : IConfiguration() {
}