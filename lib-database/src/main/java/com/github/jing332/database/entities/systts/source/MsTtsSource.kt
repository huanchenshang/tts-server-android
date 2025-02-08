package com.github.jing332.database.entities.systts.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ms")
data class MsTtsSource(
    val api: Int = API_EDGE_NATIVE,
    override val voice: String = DEFAULT_VOICE,
    override val locale: String = DEFAULT_LOCALE,
    val format: String = "audio-24khz-48kbitrate-mono-mp3",
    val speed: Float = SPEED_FOLLOW,
    val pitch: Float = PITCH_FOLLOW,
    val volume: Float = VOLUME_FOLLOW

) : ITtsSource() {
    companion object {
        const val API_EDGE_NATIVE = 0
        const val API_EDGE_OKHTTP = 3

        const val SPEED_FOLLOW = 0f
        const val PITCH_FOLLOW = 0f
        const val VOLUME_FOLLOW = 0f

        const val DEFAULT_LOCALE = "zh-CN"
        const val DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"

        fun Int.msTtsToFloat(): Float = (this / 100f) + 1f
    }

}